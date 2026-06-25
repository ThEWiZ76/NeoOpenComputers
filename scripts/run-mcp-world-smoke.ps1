param(
    [int] $TimeoutSeconds = 240,
    [string] $WorldName = 'NeoOCSmoke',
    [string] $McpServerModPath = 'M:\development\mcp-server-mod\build\libs\mcp-server-mod-neoforge-1.1.0+neoforge.mc1.21.1.jar',
    [string] $McpUrl = 'http://localhost:8080/mcp',
    [switch] $KeepClient,
    [switch] $KeepExtraMod
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir
$gradlew = Join-Path $repoRoot 'gradlew.bat'
$clientRunDir = Join-Path $repoRoot 'run\client'
$clientModsDir = Join-Path $clientRunDir 'mods'
$clientLog = Join-Path $clientRunDir 'logs\latest.log'
$worldDir = Join-Path (Join-Path $clientRunDir 'saves') $WorldName
$outputDir = Join-Path $repoRoot 'build\mcp-world-smoke'
$stdoutLog = Join-Path $outputDir 'runClient.out.log'
$stderrLog = Join-Path $outputDir 'runClient.err.log'
$toolsLog = Join-Path $outputDir 'tools-list.json'
$pingLog = Join-Path $outputDir 'ping.json'
$initializeLog = Join-Path $outputDir 'initialize.json'
$playerInfoLog = Join-Path $outputDir 'player-info.json'
$blocksLog = Join-Path $outputDir 'blocks.json'
$commandLog = Join-Path $outputDir 'command.json'
$extraModsLog = Join-Path $outputDir 'extra-mods.txt'
$nextRequestId = 1

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
Remove-Item -LiteralPath $stdoutLog, $stderrLog, $toolsLog, $pingLog, $initializeLog, $playerInfoLog, $blocksLog, $commandLog, $extraModsLog -Force -ErrorAction SilentlyContinue

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

    $body = $request | ConvertTo-Json -Depth 30 -Compress
    Invoke-RestMethod `
        -Uri $McpUrl `
        -Method Post `
        -Headers @{ Accept = 'application/json' } `
        -ContentType 'application/json' `
        -Body $body `
        -TimeoutSec 5
}

function Invoke-McpTool {
    param(
        [string] $Name,
        [object] $Arguments = @{}
    )

    Invoke-McpRequest -Method 'tools/call' -Params @{
        name = $Name
        arguments = $Arguments
    }
}

function Get-McpToolText {
    param([object] $Response)

    if ($Response.result.isError) {
        $message = if ($Response.result.content.Count -gt 0) { $Response.result.content[0].text } else { 'unknown MCP tool error' }
        throw $message
    }
    if ($Response.result.content.Count -eq 0) {
        throw 'MCP tool response had no content.'
    }
    return $Response.result.content[0].text
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
if (-not (Test-Path -LiteralPath $worldDir)) {
    throw "MCP world smoke requires an existing local save at $worldDir. Create a singleplayer test world with folder/name '$WorldName', then rerun this script."
}

New-Item -ItemType Directory -Force -Path $clientModsDir | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$source = (Resolve-Path -LiteralPath $McpServerModPath).Path
$copiedExtraMod = Join-Path $clientModsDir "mcp-world-smoke-$timestamp-$([System.IO.Path]::GetFileName($source))"
Copy-Item -LiteralPath $source -Destination $copiedExtraMod -Force
"$source -> $copiedExtraMod" | Set-Content -LiteralPath $extraModsLog -Encoding UTF8

$startedAt = Get-Date
$process = $null
try {
    if ($WorldName.Contains('"')) {
        throw 'WorldName must not contain a double quote character.'
    }
    $quickPlayProperty = "-Pneoopencomputers.quickPlayWorld=$WorldName"
    $process = Start-Process `
        -FilePath $gradlew `
        -ArgumentList @('runClient', '--no-daemon', '--console=plain', $quickPlayProperty) `
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
        throw "MCP world smoke timed out before ping succeeded at $McpUrl. Last ping error: $lastPingError. Logs: $clientLog, $stdoutLog, $stderrLog"
    }
    $pingResponse | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $pingLog -Encoding UTF8

    $initializeResponse = Invoke-McpRequest -Method 'initialize' -Params @{
        protocolVersion = '2025-06-18'
        capabilities = @{}
        clientInfo = @{
            name = 'neoopencomputers-world-smoke'
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

    $playerInfoResponse = $null
    $playerInfo = $null
    $lastPlayerInfoError = ''
    while ((Get-Date) -lt $deadline) {
        if ($process.HasExited) {
            throw "Development client exited before get_player_info proved world entry. Exit code: $($process.ExitCode). Logs: $stdoutLog, $stderrLog"
        }

        try {
            $playerInfoResponse = Invoke-McpTool -Name 'get_player_info'
            $playerInfoText = Get-McpToolText $playerInfoResponse
            $playerInfo = $playerInfoText | ConvertFrom-Json
            if ($null -ne $playerInfo.dimension -and $null -ne $playerInfo.blockPosition) {
                break
            }
        } catch {
            $lastPlayerInfoError = $_.Exception.Message
        }
        Start-Sleep -Seconds 2
    }

    if ($null -eq $playerInfo -or $null -eq $playerInfo.dimension) {
        throw "MCP world smoke reached MCP but not an in-world player for '$WorldName'. Last error: $lastPlayerInfoError. Logs: $clientLog, $stdoutLog, $stderrLog"
    }
    $playerInfoResponse | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $playerInfoLog -Encoding UTF8

    $x = [int] $playerInfo.blockPosition.x
    $y = [int] $playerInfo.blockPosition.y
    $z = [int] $playerInfo.blockPosition.z
    $blockResponse = Invoke-McpTool -Name 'get_blocks_in_area' -Arguments @{
        from = @{ x = $x; y = ($y - 1); z = $z }
        to = @{ x = $x; y = $y; z = $z }
    }
    Get-McpToolText $blockResponse | Out-Null
    $blockResponse | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $blocksLog -Encoding UTF8

    $commandResponse = Invoke-McpTool -Name 'execute_commands' -Arguments @{
        commands = @('time query daytime')
        validate_safety = $true
    }
    Get-McpToolText $commandResponse | Out-Null
    $commandResponse | ConvertTo-Json -Depth 50 | Set-Content -LiteralPath $commandLog -Encoding UTF8

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
        throw "MCP world smoke found hard failure patterns: $($matches -join ', '). Logs: $clientLog, $stdoutLog, $stderrLog"
    }

    Write-Host "MCP world smoke entered '$WorldName' and verified player info, block scan, and command transport."
    Write-Host "Player: $($playerInfo.name) in $($playerInfo.dimension) at $x $y $z"
    Write-Host "Logs: $outputDir"
} finally {
    if (-not $KeepClient) {
        Stop-SmokeProcessTree $process
    }
    if (-not $KeepExtraMod) {
        Remove-Item -LiteralPath $copiedExtraMod -Force -ErrorAction SilentlyContinue
    }
}
