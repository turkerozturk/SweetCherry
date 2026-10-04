[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Launch4jHome,
    [Parameter(Mandatory)][string]$IsccPath,
    [Parameter(Mandatory)][string]$RuntimeArchive,
    [Parameter(Mandatory)][string]$RuntimeSha256,
    [Parameter(Mandatory)][string]$RuntimeVersion
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot/../..").Path
$bundle = Join-Path $repo 'release/SweetCherry'
$out = Join-Path $repo 'dist/windows'
# The existing Maven distribution embeds YAML in the JAR; provide its filtered copy for this bundle.
if (!(Test-Path (Join-Path $bundle 'application.yml'))) {
    $filteredConfig = Join-Path $repo 'target/classes/application.yml'
    if (!(Test-Path $filteredConfig)) { throw 'Run Maven package before Windows bundling.' }
    Copy-Item $filteredConfig (Join-Path $bundle 'application.yml')
}
# Never package a developer's existing release folder; the workflow starts from a clean checkout.
foreach ($name in @('SweetCherry.jar','application.yml','LICENSE','CTBDATA/demo.ctb','allTenants/demo.txt')) {
    if (!(Test-Path (Join-Path $bundle $name))) { throw "Missing distribution file: $name. Run Maven package first." }
}
if (Test-Path (Join-Path $bundle 'login-credentials.properties')) { throw 'Do not package an already-used distribution.' }
if ((Get-FileHash $RuntimeArchive -Algorithm SHA256).Hash -ne $RuntimeSha256) { throw 'Runtime checksum mismatch.' }
[xml]$pom = Get-Content (Join-Path $repo 'pom.xml') -Raw
$version = [string]$pom.project.version
if ($version -notmatch '^(\d+)\.(\d+)\.(\d+)(?:-[A-Za-z0-9.-]+)?$') { throw "Unsupported project version: $version" }
$fileVersion = "$($Matches[1]).$($Matches[2]).$($Matches[3]).0"
New-Item $out -ItemType Directory -Force | Out-Null
$unpack = Join-Path $out 'runtime-unpack'
if (Test-Path $unpack) { Remove-Item $unpack -Recurse -Force }
New-Item $unpack -ItemType Directory | Out-Null
Expand-Archive $RuntimeArchive $unpack
$runtimeRoots = @(Get-ChildItem $unpack -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'bin/java.exe') })
if ($runtimeRoots.Count -ne 1) { throw 'Expected one Java runtime root.' }
$runtime = Join-Path $bundle 'runtime'
if (Test-Path $runtime) { throw 'runtime already exists; use a fresh staging directory.' }
Move-Item $runtimeRoots[0].FullName $runtime
$runtimeInfo = (& "$runtime/bin/java.exe" -XshowSettings:properties -version 2>&1 | Out-String)
if ($LASTEXITCODE -ne 0 -or $runtimeInfo -notmatch 'os.arch\s*=\s*amd64' -or $runtimeInfo -notmatch 'java.version\s*=\s*17\.') {
    throw 'The bundle must contain a Windows x64 Java 17 runtime.'
}
Copy-Item "$PSScriptRoot/SweetCherry.ico" $bundle
[xml]$config = Get-Content "$PSScriptRoot/launch4j.xml" -Raw
$config.launch4jConfig.outfile = (Join-Path $bundle 'SweetCherry.exe')
$config.launch4jConfig.icon = (Join-Path $bundle 'SweetCherry.ico')
$versionInfo = $config.CreateElement('versionInfo')
foreach ($entry in ([ordered]@{
    fileVersion=$fileVersion; txtFileVersion=$version; fileDescription='SweetCherry Windows x64 launcher';
    copyright='2024 Turker Ozturk'; productVersion=$fileVersion; txtProductVersion=$version;
    productName='SweetCherry'; internalName='SweetCherry'; originalFilename='SweetCherry.exe'
}).GetEnumerator()) {
    $node = $config.CreateElement($entry.Key); $node.InnerText = $entry.Value; [void]$versionInfo.AppendChild($node)
}
[void]$config.launch4jConfig.AppendChild($versionInfo)
$configPath = Join-Path $out 'launch4j.xml'
$config.Save($configPath)
& "$Launch4jHome/launch4jc.exe" $configPath
if ($LASTEXITCODE -ne 0 -or !(Test-Path "$bundle/SweetCherry.exe")) { throw 'Launch4j failed.' }
$commit = (& git -C $repo rev-parse HEAD).Trim()
@{
    applicationVersion=$version; commit=$commit; architecture='windows-x64'; java=$RuntimeVersion;
    runtimeArchiveSha256=$RuntimeSha256.ToLowerInvariant(); launch4j='3.50';
    innoSetup=(Get-Item $IsccPath).VersionInfo.FileVersion
} | ConvertTo-Json | Set-Content "$bundle/windows-bundle.json" -Encoding utf8
& $IsccPath "/DSourceDir=$bundle" "/DOutputDir=$out" "/DAppVersion=$version" "/DFileVersion=$fileVersion" "$PSScriptRoot/SweetCherry.iss"
if ($LASTEXITCODE -ne 0) { throw 'Inno Setup compilation failed.' }
Compress-Archive -Path $bundle -DestinationPath "$out/SweetCherry-$version-windows-x64.zip" -Force
Get-ChildItem $out -File | Where-Object { $_.Extension -in @('.exe','.zip') } | ForEach-Object {
    "$( (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() )  $($_.Name)"
} | Set-Content "$out/SHA256SUMS.txt" -Encoding ascii
