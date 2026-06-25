param(
    [string] $Timestamp = '',
    [switch] $SkipBuild,
    [switch] $NoZip,
    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $repoRoot 'gradlew.bat'
$outputRoot = Join-Path $repoRoot 'build\first-smoke-kits'

if ([string]::IsNullOrWhiteSpace($Timestamp)) {
    $Timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
}

$versionLine = Get-Content -LiteralPath (Join-Path $repoRoot 'gradle.properties') |
    Where-Object { $_ -like 'mod_version=*' } |
    Select-Object -First 1
$modVersion = $versionLine.Substring('mod_version='.Length).Trim()
$kitDir = Join-Path $outputRoot "neoopencomputers-first-smoke-$modVersion-$Timestamp"
$libsDir = Join-Path $repoRoot 'build\libs'

$artifacts = @(
    "neoopencomputers-$modVersion-all.jar",
    "neoopencomputers-$modVersion.jar",
    "neoopencomputers-$modVersion-api.jar",
    "neoopencomputers-$modVersion-javadoc.jar"
)

Write-Host "First smoke kit timestamp: $Timestamp"
Write-Host "Kit directory: $kitDir"

if ($DryRun) {
    Write-Host "Dry run artifact list:"
    $artifacts | ForEach-Object { Write-Host " - $_" }
    Write-Host 'Dry run complete. Build was not run and no kit was written.'
    return
}

if (-not $SkipBuild) {
    & $gradlew test build --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

New-Item -ItemType Directory -Force -Path $kitDir | Out-Null

$missing = @()
foreach ($artifact in $artifacts) {
    $path = Join-Path $libsDir $artifact
    if (Test-Path -LiteralPath $path) {
        Copy-Item -LiteralPath $path -Destination (Join-Path $kitDir $artifact) -Force
    } else {
        $missing += $artifact
    }
}

if ($missing.Count -gt 0) {
    throw "Missing first-smoke kit artifacts: $($missing -join ', ')"
}

$commit = (& git -C $repoRoot rev-parse --short=9 HEAD).Trim()
$checksums = foreach ($artifact in $artifacts) {
    $copied = Join-Path $kitDir $artifact
    $hash = Get-FileHash -LiteralPath $copied -Algorithm SHA256
    "$($hash.Hash.ToLowerInvariant())  $artifact"
}

$checksumsPath = Join-Path $kitDir 'SHA256SUMS.txt'
$checksums | Set-Content -LiteralPath $checksumsPath -Encoding UTF8

@"
# NeoOpenComputers First Smoke Kit

Version: $modVersion
Commit: $commit
Generated: $Timestamp

## Install

Use neoopencomputers-$modVersion-all.jar for first smoke testing. It includes the bundled runtime libraries needed by the mod.

Copy that jar into a NeoForge 1.21.1 client mods folder, start a local world, and run the first-smoke checklist from the repository README.

## Developer Artifacts

- neoopencomputers-$modVersion.jar is the plain mod jar.
- neoopencomputers-$modVersion-api.jar contains the public API sources/classes for addon development.
- neoopencomputers-$modVersion-javadoc.jar contains generated public API docs.

## Evidence

After testing from the dev workspace, run:

    .\scripts\run-first-smoke-client.ps1

That launcher starts the client and packages logs/screenshots after exit.

Before pushing any smoke-test changes, keep GitHub Actions disabled:

    .\scripts\check-actions-disabled.ps1

For MCP-assisted local smoke testing from the dev workspace, run:

    .\scripts\run-first-smoke-client.ps1 -WithLocalMcpServerMod

The helper jar is expected at M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar. Use -ExtraMod <path> for a different helper jar. MCP/helper evidence manifests include helper jar SHA-256, byte length, and UTC timestamp.

For a bounded MCP helper startup smoke, run:

    .\scripts\run-mcp-client-smoke.ps1

For a bounded MCP in-world smoke, create a local singleplayer save at run\client\saves\NeoOCSmoke, then run:

    .\scripts\run-mcp-world-smoke.ps1

This launches with --quickPlaySingleplayer NeoOCSmoke and writes player-info, block-scan, and command evidence under build\mcp-world-smoke.

For a bounded MCP device placement smoke in that same save, run:

    .\scripts\run-mcp-device-smoke.ps1

This writes placement command and block-scan evidence under build\mcp-device-smoke for core and peripheral blocks.

## Checksums

See SHA256SUMS.txt.
"@ | Set-Content -LiteralPath (Join-Path $kitDir 'README-FIRST-SMOKE.md') -Encoding UTF8

if (-not $NoZip) {
    $zipPath = "$kitDir.zip"
    if (Test-Path -LiteralPath $zipPath) {
        Remove-Item -LiteralPath $zipPath -Force
    }
    Compress-Archive -Path (Join-Path $kitDir '*') -DestinationPath $zipPath -Force
    Write-Host "First smoke kit: $zipPath"
} else {
    Write-Host "First smoke kit: $kitDir"
}

Write-Host "Checksums: $checksumsPath"
