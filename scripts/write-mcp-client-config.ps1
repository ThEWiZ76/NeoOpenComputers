param(
    [Parameter(Mandatory = $true)]
    [string] $ClientRunDir,
    [string] $McpUrl = 'http://localhost:8080/mcp'
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$templatePath = Join-Path $scriptDir 'mcp-client.example.json'
$configDir = Join-Path $ClientRunDir 'config'
$configPath = Join-Path $configDir 'mcp-client.json'

if (-not (Test-Path -LiteralPath $templatePath)) {
    throw "MCP client config template not found: $templatePath"
}

$uri = [Uri] $McpUrl
$config = Get-Content -LiteralPath $templatePath -Raw | ConvertFrom-Json
$config.server.host = if ([string]::IsNullOrWhiteSpace($uri.Host)) { 'localhost' } else { $uri.Host }
$config.server.port = $uri.Port
$config.server.transport = if ($uri.Scheme -eq 'https') { 'https' } else { 'http' }
$config.server.enableSafety = $true
$config.server.enableUnsafeChatCommands = $true
$config.server.enableGuiAutomationTools = $true
$config.server.autoStart = $true

New-Item -ItemType Directory -Force -Path $configDir | Out-Null
$config | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $configPath -Encoding UTF8

Write-Output $configPath
