param(
    [string] $Timestamp = '',
    [string] $McpServerModPath = 'M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar',
    [switch] $SkipBuild,
    [switch] $NoZip,
    [switch] $DryRun
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $repoRoot 'gradlew.bat'
$outputRoot = Join-Path $repoRoot 'build\first-smoke-kits'
$mcpConfigExample = Join-Path $scriptDir 'mcp-client.example.json'

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
    if (Test-Path -LiteralPath $McpServerModPath) {
        Write-Host "Optional MCP helper jar:"
        Write-Host " - $McpServerModPath"
    } else {
        Write-Host "Optional MCP helper jar not found: $McpServerModPath"
    }
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

$optionalArtifacts = @()
if (Test-Path -LiteralPath $McpServerModPath) {
    $helperSource = (Resolve-Path -LiteralPath $McpServerModPath).Path
    $helperName = [System.IO.Path]::GetFileName($helperSource)
    $helperDir = Join-Path $kitDir 'optional-mcp-helper'
    New-Item -ItemType Directory -Force -Path $helperDir | Out-Null
    $helperDestination = Join-Path $helperDir $helperName
    Copy-Item -LiteralPath $helperSource -Destination $helperDestination -Force
    $optionalArtifacts += [pscustomobject]@{
        Name = "optional-mcp-helper/$helperName"
        Path = $helperDestination
        Source = $helperSource
    }
    $helperItem = Get-Item -LiteralPath $helperSource
    @(
        "source=$helperSource",
        "sha256=$((Get-FileHash -LiteralPath $helperSource -Algorithm SHA256).Hash)",
        "Length=$($helperItem.Length)",
        "LastWriteTimeUtc=$($helperItem.LastWriteTimeUtc.ToString('o'))"
    ) | Set-Content -LiteralPath (Join-Path $helperDir 'MCP-HELPER-MANIFEST.txt') -Encoding UTF8
    $optionalArtifacts += [pscustomobject]@{
        Name = 'optional-mcp-helper/MCP-HELPER-MANIFEST.txt'
        Path = (Join-Path $helperDir 'MCP-HELPER-MANIFEST.txt')
        Source = 'generated'
    }
    if (Test-Path -LiteralPath $mcpConfigExample) {
        $configDestination = Join-Path $helperDir 'mcp-client.example.json'
        Copy-Item -LiteralPath $mcpConfigExample -Destination $configDestination -Force
        $optionalArtifacts += [pscustomobject]@{
            Name = 'optional-mcp-helper/mcp-client.example.json'
            Path = $configDestination
            Source = $mcpConfigExample
        }
    }
}

$commit = (& git -C $repoRoot rev-parse --short=9 HEAD).Trim()
$checksumTargets = @()
foreach ($artifact in $artifacts) {
    $checksumTargets += [pscustomobject]@{
        Name = $artifact
        Path = (Join-Path $kitDir $artifact)
    }
}
foreach ($artifact in $optionalArtifacts) {
    $checksumTargets += [pscustomobject]@{
        Name = $artifact.Name
        Path = $artifact.Path
    }
}
$checksums = foreach ($target in $checksumTargets) {
    $hash = Get-FileHash -LiteralPath $target.Path -Algorithm SHA256
    "$($hash.Hash.ToLowerInvariant())  $($target.Name)"
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

If present, optional-mcp-helper contains the local MCP server mod helper jar, MCP-HELPER-MANIFEST.txt with source path, SHA-256, byte length, and UTC timestamp, and mcp-client.example.json. Copy the helper jar into the same mods folder only when running MCP-assisted smoke checks. Copy mcp-client.example.json to the game profile config folder as mcp-client.json before MCP-assisted testing; it enables unsafe command tools and GUI automation tools used by deeper smoke checks.

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

The helper jar is expected at M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar. Use -ExtraMod <path> for a different helper jar. MCP/helper evidence manifests include helper jar SHA-256, byte length, UTC timestamp, and the generated mcp-client.json path. The MCP smoke scripts write run\client\config\mcp-client.json automatically with unsafe command tools and GUI automation tools enabled.

For a bounded MCP helper startup smoke, run:

    .\scripts\run-mcp-client-smoke.ps1

For a bounded MCP in-world smoke, create a local singleplayer save at run\client\saves\NeoOCSmoke, then run:

    .\scripts\run-mcp-world-smoke.ps1

This launches with --quickPlaySingleplayer NeoOCSmoke and writes player-info, block-scan, and command evidence under build\mcp-world-smoke.

For a bounded MCP device placement smoke in that same save, run:

    .\scripts\run-mcp-device-smoke.ps1

This writes placement command and block-scan evidence under build\mcp-device-smoke for core and peripheral blocks.

MCP device smoke does not prove OpenOS prompt boot. The current helper can run commands, move, scan blocks, and read chat, but it cannot right-click GUIs, insert computer components, type into screens, or read screen text. Capture OpenOS prompt boot with the hands-on checklist, or extend the helper with GUI/use/inventory/screen-read tools before automating that proof.

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
