param(
    [int] $TimeoutSeconds = 120,
    [string] $McpServerModPath = 'M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar',
    [string] $McpUrl = 'http://localhost:8080/mcp',
    [switch] $KeepClient,
    [switch] $KeepExtraMod
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $repoRoot 'gradlew.bat'
$repoCommit = (& git -C $repoRoot rev-parse --short HEAD).Trim()
$clientRunDir = Join-Path $repoRoot 'run\client'
$clientModsDir = Join-Path $clientRunDir 'mods'
$clientLog = Join-Path $clientRunDir 'logs\latest.log'
$outputDir = Join-Path $repoRoot 'build\mcp-client-smoke'
$stdoutLog = Join-Path $outputDir 'runClient.out.log'
$stderrLog = Join-Path $outputDir 'runClient.err.log'
$toolsLog = Join-Path $outputDir 'tools-list.json'
$pingLog = Join-Path $outputDir 'ping.json'
$initializeLog = Join-Path $outputDir 'initialize.json'
$extraModsLog = Join-Path $outputDir 'extra-mods.txt'
$nextRequestId = 1

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Remove-Item -LiteralPath $stdoutLog, $stderrLog, $toolsLog, $pingLog, $initializeLog, $extraModsLog -Force -ErrorAction SilentlyContinue

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

function Invoke-McpRequest {
    param(
        [string] $Method,
        [object] $Params = $null
    )

    $request = [ordered]@{
        jsonrpc = '2.0'
        id = $script:nextRequestId
        method = $Method
    }
    $script:nextRequestId++
    if ($null -ne $Params) {
        $request.params = $Params
    }

    $body = $request | ConvertTo-Json -Depth 20 -Compress
    Invoke-RestMethod `
        -Uri $McpUrl `
        -Method Post `
        -Headers @{ Accept = 'application/json' } `
        -ContentType 'application/json' `
        -Body $body `
        -TimeoutSec 5
}

try {
    Invoke-McpRequest -Method 'ping' | Out-Null
    throw "MCP endpoint already active at $McpUrl before smoke launch. Stop the existing client before running this smoke."
} catch {
    if ($_.Exception.Message -like 'MCP endpoint already active*') {
        throw
    }
}

if (-not (Test-Path -LiteralPath $McpServerModPath)) {
    throw "MCP helper mod not found: $McpServerModPath"
}

New-Item -ItemType Directory -Force -Path $clientModsDir | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$source = (Resolve-Path -LiteralPath $McpServerModPath).Path
$copiedExtraMod = Join-Path $clientModsDir "mcp-client-smoke-$timestamp-$([System.IO.Path]::GetFileName($source))"
Copy-Item -LiteralPath $source -Destination $copiedExtraMod -Force
$sourceItem = Get-Item -LiteralPath $source
$sourceHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $source).Hash
@(
    "commit=$repoCommit"
    "source=$source"
    "destination=$copiedExtraMod"
    "sha256=$sourceHash"
    "Length=$($sourceItem.Length)"
    "LastWriteTimeUtc=$($sourceItem.LastWriteTimeUtc.ToString('o'))"
) | Set-Content -LiteralPath $extraModsLog -Encoding UTF8

$startedAt = Get-Date
$process = $null
try {
    $process = Start-Process `
        -FilePath $gradlew `
        -ArgumentList @('runClient', '--no-daemon', '--console=plain') `
        -WorkingDirectory $repoRoot `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -WindowStyle Hidden `
        -PassThru

    $deadline = $startedAt.AddSeconds($TimeoutSeconds)
    $pingResponse = $null
    $lastPingError = ''
    while ((Get-Date) -lt $deadline) {
        if ($process.HasExited) {
            throw "Development client exited before MCP ping succeeded. Exit code: $($process.ExitCode). Logs: $stdoutLog, $stderrLog"
        }

        try {
            $pingResponse = Invoke-McpRequest -Method 'ping'
            break
        } catch {
            $lastPingError = $_.Exception.Message
            Start-Sleep -Seconds 1
        }
    }

    if ($null -eq $pingResponse) {
        throw "MCP smoke timed out before ping succeeded at $McpUrl. Last ping error: $lastPingError. Logs: $clientLog, $stdoutLog, $stderrLog"
    }

    $pingResponse | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $pingLog -Encoding UTF8

    $initializeResponse = Invoke-McpRequest -Method 'initialize' -Params @{
        protocolVersion = '2025-06-18'
        capabilities = @{}
        clientInfo = @{
            name = 'neoopencomputers-first-smoke'
            version = '1.0.0'
        }
    }
    $initializeResponse | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $initializeLog -Encoding UTF8

    $toolsResponse = Invoke-McpRequest -Method 'tools/list'
    $toolsResponse | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $toolsLog -Encoding UTF8
    $toolNames = @($toolsResponse.result.tools | ForEach-Object { $_.name })
    foreach ($requiredTool in @('execute_commands', 'get_player_info', 'get_blocks_in_area')) {
        if (-not ($toolNames -contains $requiredTool)) {
            throw "MCP tools/list missing required tool '$requiredTool'. Tools: $($toolNames -join ', ')"
        }
    }

    $freshClientLog = ''
    if ((Test-Path -LiteralPath $clientLog) -and (Get-Item -LiteralPath $clientLog).LastWriteTime -ge $startedAt.AddSeconds(-2)) {
        $freshClientLog = Read-TextIfPresent $clientLog
    }
    $combinedLog = $freshClientLog + "`n" + (Read-TextIfPresent $stdoutLog) + "`n" + (Read-TextIfPresent $stderrLog)
    $hardFailures = @(
        '/ERROR]',
        '/FATAL]',
        'Missing texture',
        'missing-texture',
        'Unable to load model',
        'FileNotFoundException',
        'Exception loading'
    )
    $matches = @($hardFailures | Where-Object { $combinedLog.Contains($_) })
    if ($matches.Count -gt 0) {
        throw "MCP client smoke found hard failure patterns: $($matches -join ', '). Logs: $clientLog, $stdoutLog, $stderrLog"
    }

    Write-Host "MCP client smoke reached ping, initialize, and tools/list."
    Write-Host "Tools: $($toolNames -join ', ')"
    Write-Host "Logs: $outputDir"
} finally {
    if (-not $KeepClient) {
        Stop-SmokeProcessTree $process
    }
    if (-not $KeepExtraMod) {
        Remove-Item -LiteralPath $copiedExtraMod -Force -ErrorAction SilentlyContinue
    }
}
