[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$BundlePath,
    [Parameter(Mandatory)][string]$DestinationPath
)
$ErrorActionPreference = 'Stop'
$BundlePath = (Resolve-Path $BundlePath).Path
$DestinationPath = [IO.Path]::GetFullPath($DestinationPath)
# A private staging tree preserves the setup's runtime and original build metadata.
$stage = Join-Path ([IO.Path]::GetTempPath()) ("sweetcherry-portable-" + [guid]::NewGuid())
$portable = Join-Path $stage 'SweetCherry'
try {
    New-Item $portable -ItemType Directory -Force | Out-Null
    Get-ChildItem $BundlePath -Force | Where-Object { $_.Name -ne 'runtime' } | ForEach-Object {
        Copy-Item $_.FullName $portable -Recurse -Force
    }
    $info = Get-Content "$portable/build-info.json" -Raw | ConvertFrom-Json
    $info.packageRuntimeIncluded = $false
    $info | ConvertTo-Json | Set-Content "$portable/build-info.json" -Encoding utf8
    foreach ($name in @('SweetCherry.jar','SweetCherry.exe','run.bat','run.sh','run.command','launcher.sh','install-desktop.sh','LICENSE','THIRD-PARTY-NOTICES.md','licenses','CTBDATA/demo.ctb','allTenants/demo.txt')) {
        if (!(Test-Path (Join-Path $portable $name))) { throw "Missing portable file: $name" }
    }
    if (Test-Path "$portable/runtime") { throw 'The all-OS ZIP must not include Java.' }
    Compress-Archive -Path $portable -DestinationPath $DestinationPath -Force
} finally {
    if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
}
