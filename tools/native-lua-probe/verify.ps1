param([Parameter(Mandatory = $true)][string]$JavaHome)
$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$outputDir = Join-Path $repoRoot 'build/native-lua-probe'
New-Item -ItemType Directory -Force $outputDir | Out-Null
$java = Join-Path $JavaHome 'bin/java.exe'
$javac = Join-Path $JavaHome 'bin/javac.exe'
if (-not [Environment]::Is64BitProcess -or -not $IsWindows) {
    throw 'This probe runner currently targets Windows x64 only.'
}
$artifacts = @{
    'OC-JNLua-20230530.0.jar' = '41ABCE30160A8014ABD34E885F0F073FB35C8D9E2F78D230B7B9E64A79CCEDBB'
    'OC-JNLua-Natives-20220928.1.jar' = '43979D288FE06A8323FDF89E05BFEC0448C64F026762B43EA1FA65BDA3768A17'
}
foreach ($name in $artifacts.Keys) {
    $destination = Join-Path $outputDir $name
    if (-not (Test-Path -LiteralPath $destination)) {
        Invoke-WebRequest "https://asie.pl/javadeps/$name" -OutFile $destination
    }
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash -ne $artifacts[$name]) {
        throw "Artifact hash mismatch: $name"
    }
}
$api = Join-Path $outputDir 'OC-JNLua-20230530.0.jar'
& $javac -encoding UTF-8 -cp $api -d $outputDir (Join-Path $PSScriptRoot 'NativeLuaPersistenceProbe.java')
if ($LASTEXITCODE -ne 0) { throw 'Probe compilation failed.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $outputDir 'OC-JNLua-Natives-20220928.1.jar'))
try {
    foreach ($version in '52', '53', '54') {
        $name = "libjnlua$version-windows-x86_64.dll"
        $native = Join-Path $outputDir $name
        [IO.Compression.ZipFileExtensions]::ExtractToFile($archive.GetEntry("assets/opencomputers/lib/$name"), $native, $true)
        $snapshot = Join-Path $outputDir "state$version.bin"
        foreach ($mode in 'save', 'restore') {
            & $java -cp "$outputDir;$api" NativeLuaPersistenceProbe $native $version $mode $snapshot
            if ($LASTEXITCODE -ne 0) { throw "Lua $version $mode failed." }
        }
    }
} finally {
    $archive.Dispose()
}
