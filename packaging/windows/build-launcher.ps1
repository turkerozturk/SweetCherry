[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path "$PSScriptRoot/../..").Path
$bundle = Join-Path $repo 'release/SweetCherry'
$out = Join-Path $repo 'dist/windows'
if (!(Test-Path (Join-Path $bundle 'SweetCherry.jar'))) { throw 'Run mvnw.cmd clean package first.' }
New-Item $out -ItemType Directory -Force | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'SweetCherry.ico') (Join-Path $bundle 'SweetCherry.ico') -Force
[xml]$config = Get-Content (Join-Path $PSScriptRoot 'launch4j.xml') -Raw -Encoding utf8
$config.launch4jConfig.outfile = Join-Path $bundle 'SweetCherry.exe'
$config.launch4jConfig.icon = Join-Path $bundle 'SweetCherry.ico'
[xml]$pom = Get-Content (Join-Path $repo 'pom.xml') -Raw -Encoding utf8
$version = [string]$pom.project.version
if ($version -notmatch '^(\d+)\.(\d+)\.(\d+)(?:-[A-Za-z0-9.-]+)?$') { throw "Unsupported project version: $version" }
$fileVersion = "$($Matches[1]).$($Matches[2]).$($Matches[3]).0"
$versionInfo = $config.CreateElement('versionInfo')
foreach ($entry in ([ordered]@{
    fileVersion=$fileVersion; txtFileVersion=$version; fileDescription='SweetCherry Windows x64 launcher';
    copyright='2024 Turker Ozturk'; productVersion=$fileVersion; txtProductVersion=$version;
    productName='SweetCherry'; internalName='SweetCherry'; originalFilename='SweetCherry.exe'
}).GetEnumerator()) {
    $node = $config.CreateElement($entry.Key); $node.InnerText = $entry.Value; [void]$versionInfo.AppendChild($node)
}
[void]$config.launch4jConfig.AppendChild($versionInfo)
$config.Save((Join-Path $out 'launch4j.xml'))
Push-Location $repo
try {
    & "$repo/mvnw.cmd" --batch-mode -Pwindows-launcher com.akathist.maven.plugins.launch4j:launch4j-maven-plugin:2.7.0:launch4j
    if ($LASTEXITCODE -ne 0) { throw 'Launch4j compilation failed.' }
} finally { Pop-Location }
if (!(Test-Path (Join-Path $bundle 'SweetCherry.exe'))) { throw 'The launcher output was not created.' }
Write-Host "Launcher rebuilt: $bundle/SweetCherry.exe"
