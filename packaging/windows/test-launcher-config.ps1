[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
# Test the actual local launcher script in isolation; no JDK, Maven download or app launch.
$source = (Resolve-Path "$PSScriptRoot/../..").Path
$fixture = Join-Path ([IO.Path]::GetTempPath()) ("SweetCherry XML test " + [char]0x00fc + [char]0x00e7 + '-' + [guid]::NewGuid())
$before = (Get-Location).Path
function Write-MavenStub([int]$Code) {
    $path = Join-Path $fixture 'mvnw.cmd'
    if ($env:OS -eq 'Windows_NT') {
        [IO.File]::WriteAllText($path, "@echo off`r`necho %* > launcher-test-arguments.txt`r`nexit /b $Code`r`n")
    } else {
        [IO.File]::WriteAllText($path, "#!/bin/sh`nprintf '%s\n' `"`$@`" > launcher-test-arguments.txt`nexit $Code`n")
        & chmod +x $path
        if ($LASTEXITCODE -ne 0) { throw 'Cannot prepare the test Maven stub.' }
    }
}
try {
    $scripts = Join-Path $fixture 'packaging/windows'
    $bundle = Join-Path $fixture 'release/SweetCherry'
    New-Item $scripts,$bundle -ItemType Directory -Force | Out-Null
    foreach ($name in @('build-launcher.ps1','launch4j.xml','SweetCherry.ico')) {
        Copy-Item (Join-Path $source "packaging/windows/$name") (Join-Path $scripts $name)
    }
    [IO.File]::WriteAllText((Join-Path $fixture 'pom.xml'), '<project><version>1.13.0</version></project>')
    [IO.File]::WriteAllText((Join-Path $bundle 'SweetCherry.jar'), 'fixture only')
    [IO.File]::WriteAllText((Join-Path $bundle 'SweetCherry.exe'), 'fixture only')
    Write-MavenStub 0
    & (Join-Path $scripts 'build-launcher.ps1')
    [xml]$config = Get-Content (Join-Path $fixture 'dist/windows/launch4j.xml') -Raw -Encoding utf8
    foreach ($entry in @{outfile=(Join-Path $bundle 'SweetCherry.exe');icon=(Join-Path $bundle 'SweetCherry.ico')}.GetEnumerator()) {
        if ($config.launch4jConfig.SelectSingleNode($entry.Key).InnerText -cne [string]$entry.Value) { throw "Incorrect launcher XML value: $($entry.Key)" }
    }
    if ($config.launch4jConfig.versionInfo.fileVersion -ne '1.13.0.0') { throw 'Incorrect launcher version metadata.' }
    $arguments = [IO.File]::ReadAllText((Join-Path $fixture 'launcher-test-arguments.txt'))
    if ($arguments -notmatch '-Pwindows-launcher' -or $arguments -notmatch 'launch4j-maven-plugin:2.7.0:launch4j') { throw 'Launcher compilation was not invoked.' }
    if ((Get-Location).Path -cne $before) { throw 'The launcher build changed the caller directory.' }
    Write-MavenStub 7
    $failed = $false
    try { & (Join-Path $scripts 'build-launcher.ps1') }
    catch { if ($_.Exception.Message -notmatch 'Launch4j compilation failed') { throw }; $failed = $true }
    if (!$failed) { throw 'The launcher script ignored a Maven failure.' }
    if ((Get-Location).Path -cne $before) { throw 'The failed launcher build changed the caller directory.' }
    Write-Host 'Launcher XML regression checks passed (isolated Maven stub; spaces and Unicode paths).'
} finally {
    if (Test-Path -LiteralPath $fixture) { Remove-Item -LiteralPath $fixture -Recurse -Force }
}
# The negative Maven test intentionally leaves LASTEXITCODE=7. GitHub Actions
# propagates that value unless a successful test script exits explicitly.
# Keep this after finally: assertion and cleanup failures must still fail CI.
exit 0
