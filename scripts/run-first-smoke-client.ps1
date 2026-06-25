param(
    [string] $Timestamp = '',
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

$gradleArgs = @('runClient', '--no-daemon', '--console=plain')
$collectorArgs = @('-Timestamp', $Timestamp)
if ($NoZip) {
    $collectorArgs += '-NoZip'
}

Write-Host "First smoke timestamp: $Timestamp"
Write-Host "Client command: $gradlew $($gradleArgs -join ' ')"
Write-Host "Collector command: $collector $($collectorArgs -join ' ')"

if ($DryRun) {
    Write-Host 'Dry run complete. Client was not started.'
    return
}

$process = Start-Process `
    -FilePath $gradlew `
    -ArgumentList $gradleArgs `
    -WorkingDirectory $repoRoot `
    -Wait `
    -PassThru

if ($process.ExitCode -ne 0) {
    Write-Warning "Development client exited with code $($process.ExitCode). Collecting logs anyway."
}

& $collector @collectorArgs

if ($process.ExitCode -ne 0) {
    exit $process.ExitCode
}
