param(
    [string] $OutputDir = '',
    [string] $Timestamp = '',
    [switch] $NoZip
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

if ([string]::IsNullOrWhiteSpace($Timestamp)) {
    $Timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
}

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $repoRoot 'build\first-smoke-reports'
}

$reportDir = Join-Path $OutputDir "first-smoke-$Timestamp"
$logsDir = Join-Path $reportDir 'logs'
$screenshotsDir = Join-Path $reportDir 'screenshots'
New-Item -ItemType Directory -Force -Path $logsDir | Out-Null

function Copy-IfPresent {
    param(
        [string] $Source,
        [string] $DestinationName
    )

    if (Test-Path -LiteralPath $Source) {
        $destination = Join-Path $logsDir $DestinationName
        Copy-Item -LiteralPath $Source -Destination $destination -Force
        return $destination
    }

    return $null
}

function Read-TextIfPresent {
    param([string] $Path)

    if (Test-Path -LiteralPath $Path) {
        return Get-Content -LiteralPath $Path -Raw
    }

    return ''
}

$clientRunDir = Join-Path $repoRoot 'run\client'
$clientLatestLog = Join-Path $clientRunDir 'logs\latest.log'
$clientDebugLog = Join-Path $clientRunDir 'logs\debug.log'
$clientCrashDir = Join-Path $clientRunDir 'crash-reports'
$clientScreenshotsDir = Join-Path $clientRunDir 'screenshots'
$smokeDir = Join-Path $repoRoot 'build\client-smoke'
$smokeStdout = Join-Path $smokeDir 'runClient.out.log'
$smokeStderr = Join-Path $smokeDir 'runClient.err.log'
$sessionDir = Join-Path (Join-Path $repoRoot 'build\first-smoke-sessions') "session-$Timestamp"
$sessionStdout = Join-Path $sessionDir 'runClient.out.log'
$sessionStderr = Join-Path $sessionDir 'runClient.err.log'
$sessionExtraMods = Join-Path $sessionDir 'extra-mods.txt'

$copied = @()
$copied += Copy-IfPresent $clientLatestLog 'client-latest.log'
$copied += Copy-IfPresent $clientDebugLog 'client-debug.log'
$copied += Copy-IfPresent $smokeStdout 'bounded-smoke-stdout.log'
$copied += Copy-IfPresent $smokeStderr 'bounded-smoke-stderr.log'
$copied += Copy-IfPresent $sessionStdout 'interactive-client-stdout.log'
$copied += Copy-IfPresent $sessionStderr 'interactive-client-stderr.log'
$copied += Copy-IfPresent $sessionExtraMods 'interactive-extra-mods.txt'
$copied = @($copied | Where-Object { $null -ne $_ })

if (Test-Path -LiteralPath $clientCrashDir) {
    $crashDestination = Join-Path $logsDir 'crash-reports'
    Copy-Item -LiteralPath $clientCrashDir -Destination $crashDestination -Recurse -Force
}

$copiedScreenshots = @()
if (Test-Path -LiteralPath $clientScreenshotsDir) {
    $screenshotFiles = @(Get-ChildItem -LiteralPath $clientScreenshotsDir -File -Filter '*.png' | Sort-Object LastWriteTime -Descending | Select-Object -First 20)
    if ($screenshotFiles.Count -gt 0) {
        New-Item -ItemType Directory -Force -Path $screenshotsDir | Out-Null
        foreach ($screenshot in $screenshotFiles) {
            $destination = Join-Path $screenshotsDir $screenshot.Name
            Copy-Item -LiteralPath $screenshot.FullName -Destination $destination -Force
            $copiedScreenshots += $destination
        }
    }
}

$combinedLog = (Read-TextIfPresent $clientLatestLog) + "`n" +
    (Read-TextIfPresent $clientDebugLog) + "`n" +
    (Read-TextIfPresent $smokeStdout) + "`n" +
    (Read-TextIfPresent $smokeStderr) + "`n" +
    (Read-TextIfPresent $sessionStdout) + "`n" +
    (Read-TextIfPresent $sessionStderr) + "`n" +
    (Read-TextIfPresent $sessionExtraMods)

$failurePatterns = @(
    '/ERROR]',
    '/FATAL]',
    'Missing texture',
    'missing-texture',
    'Unable to load model',
    'FileNotFoundException',
    'Exception loading',
    'Crash report'
)
$matches = @($failurePatterns | Where-Object { $combinedLog.Contains($_) })

$status = if ($matches.Count -eq 0) { 'No hard failure patterns found in copied logs.' } else { "Hard failure patterns found: $($matches -join ', ')" }
$reportPath = Join-Path $reportDir 'summary.md'
$relativeLogs = if ($copied.Count -eq 0) { '- No standard logs were found.' } else { ($copied | ForEach-Object { "- logs/$([System.IO.Path]::GetFileName($_))" }) -join "`n" }
$relativeScreenshots = if ($copiedScreenshots.Count -eq 0) { '- No screenshots were found.' } else { ($copiedScreenshots | ForEach-Object { "- screenshots/$([System.IO.Path]::GetFileName($_))" }) -join "`n" }

@"
# NeoOpenComputers First Smoke Report

Generated: $Timestamp

## Log Scan

$status

## Copied Logs

$relativeLogs

## Copied Screenshots

$relativeScreenshots

## Tester Checklist

- [ ] Client opens local world with NeoOpenComputers installed.
- [ ] Computer case, screen, keyboard, disk drive, modem, redstone card, printer, and print block place without crash.
- [ ] OpenOS or Lua prompt boots on a placed computer.
- [ ] Filesystem, EEPROM, floppy, and disk-drive actions work once.
- [ ] Screen output and keyboard input survive save/reload.
- [ ] Terminal item key input, paste, mouse click/drag/release, and scroll reach the bound computer.
- [ ] Disk-drive floppy data survives save/reload.
- [ ] Redstone, modem, storage, inventory, tank, and transposer each get one basic smoke pass.
- [ ] Printer creates a print item and placed print renders configured shape data.
- [ ] Screenshots exist for visual print, screen, GUI, or texture issues.
- [ ] Placed print rotation, drops, hit boxes, tooltip/name, light/opacity, and redstone/button activation look sane.
- [ ] No crash, missing texture, untranslated key, client/server error, or unexpected visual behavior remains unexplained.

## Notes

Record world seed, reproduction steps, screenshots, and any unexpected log lines here.
"@ | Set-Content -LiteralPath $reportPath -Encoding UTF8

if (-not $NoZip) {
    $zipPath = "$reportDir.zip"
    if (Test-Path -LiteralPath $zipPath) {
        Remove-Item -LiteralPath $zipPath -Force
    }
    Compress-Archive -Path (Join-Path $reportDir '*') -DestinationPath $zipPath -Force
    Write-Host "First smoke report: $zipPath"
} else {
    Write-Host "First smoke report: $reportDir"
}

Write-Host "Summary: $reportPath"
if ($matches.Count -gt 0) {
    Write-Warning $status
}
