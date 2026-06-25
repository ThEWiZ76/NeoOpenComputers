param(
    [int] $TimeoutSeconds = 90,
    [switch] $KeepClient
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $repoRoot 'gradlew.bat'
$clientRunDir = Join-Path $repoRoot 'run\client'
$clientLog = Join-Path $clientRunDir 'logs\latest.log'
$outputDir = Join-Path $repoRoot 'build\client-smoke'
$stdoutLog = Join-Path $outputDir 'runClient.out.log'
$stderrLog = Join-Path $outputDir 'runClient.err.log'

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Remove-Item -LiteralPath $stdoutLog, $stderrLog -Force -ErrorAction SilentlyContinue

function Stop-SmokeProcessTree {
    param([System.Diagnostics.Process] $Process)

    if ($null -eq $Process -or $Process.HasExited) {
        return
    }

    & taskkill.exe /PID $Process.Id /T /F | Out-Null
}

function Read-TextIfPresent {
    param([string] $Path)

    if (Test-Path -LiteralPath $Path) {
        return Get-Content -LiteralPath $Path -Raw
    }
    return ''
}

$startedAt = Get-Date
$process = Start-Process `
    -FilePath $gradlew `
    -ArgumentList @('runClient', '--no-daemon', '--console=plain') `
    -WorkingDirectory $repoRoot `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

$deadline = $startedAt.AddSeconds($TimeoutSeconds)
$sawResourceReload = $false
$sawBlockAtlas = $false

while ((Get-Date) -lt $deadline) {
    if ($process.HasExited) {
        break
    }

    if (Test-Path -LiteralPath $clientLog) {
        $logInfo = Get-Item -LiteralPath $clientLog
        if ($logInfo.LastWriteTime -ge $startedAt.AddSeconds(-2)) {
            $text = Read-TextIfPresent $clientLog
            $sawResourceReload = $sawResourceReload -or $text.Contains('Reloading ResourceManager')
            $sawBlockAtlas = $sawBlockAtlas -or $text.Contains('minecraft:textures/atlas/blocks.png-atlas')
            if ($sawResourceReload -and $sawBlockAtlas) {
                break
            }
        }
    }

    Start-Sleep -Seconds 1
}

$combinedLog = (Read-TextIfPresent $clientLog) + "`n" + (Read-TextIfPresent $stdoutLog) + "`n" + (Read-TextIfPresent $stderrLog)
$hardFailures = @(
    '/ERROR]',
    '/FATAL]',
    'Missing texture',
    'missing-texture',
    'Unable to load model',
    'FileNotFoundException',
    'Exception loading'
)
$matches = $hardFailures | Where-Object { $combinedLog.Contains($_) }

if (-not $KeepClient) {
    Stop-SmokeProcessTree $process
}

if (-not ($sawResourceReload -and $sawBlockAtlas)) {
    Write-Error "Client smoke timed out before resource reload and block atlas creation. Logs: $clientLog, $stdoutLog, $stderrLog"
}

if ($matches.Count -gt 0) {
    Write-Error "Client smoke found hard failure patterns: $($matches -join ', '). Logs: $clientLog, $stdoutLog, $stderrLog"
}

Write-Host "Client smoke reached resource reload and block atlas creation."
Write-Host "Logs: $clientLog"
