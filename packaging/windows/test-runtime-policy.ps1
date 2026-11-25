$ErrorActionPreference = 'Stop'
. "$PSScriptRoot/runtime-policy.ps1"
foreach ($version in @('17.0.21.1','17.0.22','17.1.0')) {
    Assert-BundleJavaRuntime "os.arch = amd64`njava.version = $version`n"
}
foreach ($version in @('17.0.21','17.0.20','21.0.13','17.0.21.1-ea','unknown')) {
    $rejected = $false
    try { Assert-BundleJavaRuntime "os.arch = amd64`njava.version = $version`n" } catch { $rejected = $true }
    if (!$rejected) { throw "Accepted unapproved runtime $version" }
}
$rejected = $false
try { Assert-BundleJavaRuntime "os.arch = aarch64`njava.version = 17.0.21.1`n" } catch { $rejected = $true }
if (!$rejected) { throw 'Accepted the wrong architecture.' }
Write-Host 'Bundle Java security baseline checks passed.'
exit 0
