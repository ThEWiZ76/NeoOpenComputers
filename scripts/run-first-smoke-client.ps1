param(
    [string] $Timestamp = '',
    [string[]] $ExtraMod = @(),
    [switch] $WithLocalMcpServerMod,
    [string] $McpServerModPath = 'M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar',
    [switch] $KeepExtraMods,
    [switch] $NoZip,
    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $repoRoot 'gradlew.bat'
$collector = Join-Path $scriptDir 'collect-first-smoke-report.ps1'

if ([string]::IsNullOrWhiteSpace($Timestamp)) {
    $Timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
}

$sessionDir = Join-Path (Join-Path $repoRoot 'build\first-smoke-sessions') "session-$Timestamp"
$stdoutLog = Join-Path $sessionDir 'runClient.out.log'
$stderrLog = Join-Path $sessionDir 'runClient.err.log'
$extraModsLog = Join-Path $sessionDir 'extra-mods.txt'
$clientModsDir = Join-Path $repoRoot 'run\client\mods'
$gradleArgs = @('runClient', '--no-daemon', '--console=plain')
$collectorArgs = @('-Timestamp', $Timestamp)
if ($NoZip) {
    $collectorArgs += '-NoZip'
}

if ($WithLocalMcpServerMod) {
    $ExtraMod += $McpServerModPath
}
$extraMods = @($ExtraMod | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })

Write-Host "First smoke timestamp: $Timestamp"
Write-Host "Session logs: $sessionDir"
Write-Host "Client command: $gradlew $($gradleArgs -join ' ')"
Write-Host "Collector command: $collector $($collectorArgs -join ' ')"
if ($extraMods.Count -gt 0) {
    Write-Host "Extra mods: $($extraMods -join ', ')"
    Write-Host "Extra mod cleanup: $(if ($KeepExtraMods) { 'keep copied jars' } else { 'remove copied jars after client exit' })"
}

if ($DryRun) {
    foreach ($extraMod in $extraMods) {
        if (-not (Test-Path -LiteralPath $extraMod)) {
            throw "Extra mod not found: $extraMod"
        }
    }
    Write-Host 'Dry run complete. Client was not started.'
    return
}

New-Item -ItemType Directory -Force -Path $sessionDir | Out-Null
Remove-Item -LiteralPath $stdoutLog, $stderrLog -Force -ErrorAction SilentlyContinue

$copiedExtraMods = @()
$processExitCode = 0
try {
    if ($extraMods.Count -gt 0) {
        New-Item -ItemType Directory -Force -Path $clientModsDir | Out-Null
        $extraModLines = @()
        foreach ($extraMod in $extraMods) {
            if (-not (Test-Path -LiteralPath $extraMod)) {
                throw "Extra mod not found: $extraMod"
            }
            if ([System.IO.Path]::GetExtension($extraMod) -ne '.jar') {
                throw "Extra mod must be a jar: $extraMod"
            }

            $source = (Resolve-Path -LiteralPath $extraMod).Path
            $destinationName = "first-smoke-extra-$Timestamp-$([System.IO.Path]::GetFileName($source))"
            $destination = Join-Path $clientModsDir $destinationName
            Copy-Item -LiteralPath $source -Destination $destination -Force
            $copiedExtraMods += $destination
            $sourceItem = Get-Item -LiteralPath $source
            $sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $source).Hash
            $extraModLines += "source=$source"
            $extraModLines += "destination=$destination"
            $extraModLines += "sha256=$sourceHash"
            $extraModLines += "Length=$($sourceItem.Length)"
            $extraModLines += "LastWriteTimeUtc=$($sourceItem.LastWriteTimeUtc.ToString('o'))"
            $extraModLines += ''
        }
        $extraModLines | Set-Content -LiteralPath $extraModsLog -Encoding UTF8
    } else {
        'No extra mods copied.' | Set-Content -LiteralPath $extraModsLog -Encoding UTF8
    }

    $process = Start-Process `
        -FilePath $gradlew `
        -ArgumentList $gradleArgs `
        -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -Wait `
        -PassThru

    $processExitCode = $process.ExitCode
    if ($processExitCode -ne 0) {
        Write-Warning "Development client exited with code $processExitCode. Collecting logs anyway."
    }

    & $collector @collectorArgs
} finally {
    if (-not $KeepExtraMods) {
        foreach ($copiedExtraMod in $copiedExtraMods) {
            Remove-Item -LiteralPath $copiedExtraMod -Force -ErrorAction SilentlyContinue
        }
    }
}

if ($processExitCode -ne 0) {
    exit $processExitCode
}
