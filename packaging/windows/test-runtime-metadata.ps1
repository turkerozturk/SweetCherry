$ErrorActionPreference = 'Stop'
# Execute the exact metadata-selection code in the workflow against offline API-shaped fixtures.
$workflow = Get-Content -LiteralPath (Join-Path $PSScriptRoot '../../.github/workflows/windows-bundle.yml') -Raw
$match = [regex]::Match($workflow, "(?ms)^          Write-Host 'Reading Temurin runtime metadata\.'\r?\n(?<code>.*?)^          \`$archive =")
if (!$match.Success) { throw 'Cannot find workflow metadata selection block.' }
$code = [scriptblock]::Create(($match.Groups['code'].Value -replace '(?m)^          ', ''))
function Invoke-RestMethod([string]$Uri) {
    if ($Uri -ne 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse') { throw 'Unexpected metadata endpoint.' }
    return $script:fixture
}
function New-Fixture([string]$Version) {
    # /assets/latest returns BinaryAssetView.version, not Release.version_data.
    return ('{"release_name":"jdk-' + $Version + '","version":{"openjdk_version":"' + $Version + '"},"binary":{"package":{"link":"https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.21.1%2B2/runtime.zip","checksum":"' + ('a' * 64) + '"}}}') | ConvertFrom-Json
}
foreach ($version in @('17.0.21.1+2','17.0.22+7','17.1.0+1')) {
    $script:fixture = New-Fixture $version
    & $code
}
$bad = @(
    (New-Fixture '17.0.21+9'),
    (New-Fixture '17.0.21.1-ea+2'),
    (New-Fixture '21.0.13.1+2'),
    (New-Fixture '')
)
$missing = New-Fixture '17.0.21.1+2'
$missing.PSObject.Properties.Remove('version')
$missing | Add-Member -NotePropertyName version_data -NotePropertyValue ([pscustomobject]@{openjdk_version='17.0.21.1+2'})
$bad += $missing
$claimed = New-Fixture '17.0.21+9'; $claimed.release_name = 'jdk-17.0.21.1+2'; $bad += $claimed
$untrusted = New-Fixture '17.0.21.1+2'; $untrusted.binary.package.link = 'https://example.org/runtime.zip'; $bad += $untrusted
foreach ($item in $bad) {
    $script:fixture = $item; $rejected = $false
    try { & $code } catch { $rejected = $true }
    if (!$rejected) { throw 'Workflow accepted invalid/unpatched metadata.' }
}
$script:fixture = @((New-Fixture '17.0.21.1+2'),(New-Fixture '17.0.22+7'));$rejected=$false
try { & $code } catch { $rejected=$true }
if (!$rejected) { throw 'Workflow accepted ambiguous metadata.' }
Write-Host 'Temurin /assets/latest metadata regression checks passed (offline; real workflow block).'
exit 0
