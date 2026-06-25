param(
    [string] $RemoteRef = 'origin/develop',
    [switch] $SkipFetch
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

function Get-GitTreeEntries {
    param(
        [string] $Ref,
        [string] $Path
    )

    $output = & git -C $repoRoot ls-tree -r --name-only $Ref $Path 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect git tree '$Ref' path '$Path'."
    }
    return @($output | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

if (-not $SkipFetch) {
    & git -C $repoRoot fetch origin develop | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not fetch origin/develop before checking GitHub Actions guard.'
    }
}

$localWorkflowDir = Join-Path $repoRoot '.github\workflows'
$localWorkflows = @()
if (Test-Path -LiteralPath $localWorkflowDir) {
    $localWorkflows = @(Get-ChildItem -LiteralPath $localWorkflowDir -File -Recurse | ForEach-Object { $_.FullName })
}

$headWorkflows = Get-GitTreeEntries -Ref 'HEAD' -Path '.github/workflows'
$remoteWorkflows = Get-GitTreeEntries -Ref $RemoteRef -Path '.github/workflows'

$failures = @()
if ($localWorkflows.Count -gt 0) {
    $failures += "local workflow files: $($localWorkflows -join ', ')"
}
if ($headWorkflows.Count -gt 0) {
    $failures += "HEAD workflow files: $($headWorkflows -join ', ')"
}
if ($remoteWorkflows.Count -gt 0) {
    $failures += "$RemoteRef workflow files: $($remoteWorkflows -join ', ')"
}

if ($failures.Count -gt 0) {
    throw "GitHub Actions disabled guard failed. Remove workflow files before pushing. $($failures -join '; ')"
}

$localGithubExists = Test-Path -LiteralPath (Join-Path $repoRoot '.github')
$headGithubEntries = Get-GitTreeEntries -Ref 'HEAD' -Path '.github'
$remoteGithubEntries = Get-GitTreeEntries -Ref $RemoteRef -Path '.github'

Write-Host 'GitHub Actions disabled guard clean.'
Write-Host "local .github exists: $localGithubExists"
Write-Host "local workflow files: $($localWorkflows.Count)"
Write-Host "HEAD .github entries: $($headGithubEntries.Count)"
Write-Host "HEAD workflow files: $($headWorkflows.Count)"
Write-Host "$RemoteRef .github entries: $($remoteGithubEntries.Count)"
Write-Host "$RemoteRef workflow files: $($remoteWorkflows.Count)"
