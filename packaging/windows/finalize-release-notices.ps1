# Documentation-only repack: use existing application/runtime binaries from the draft.
$ErrorActionPreference = 'Stop'
$tag = 'v1.0.0'
$version = '1.0.0'
$expectedCommit = '8f88bc943d3b1b772184dfd4d3936c5424e5fbce'
$repo = $env:GITHUB_REPOSITORY
if (!$repo) { throw 'Run this script in the GitHub Actions workflow.' }
$releaseText = & gh api "repos/$repo/releases?per_page=100" --jq '.[] | select(.tag_name=="v1.0.0")'
if ($LASTEXITCODE -ne 0) { throw 'Release draft was not found.' }
$matchingReleases = @($releaseText | ConvertFrom-Json)
if ($matchingReleases.Count -ne 1) { throw 'Expected exactly one v1.0.0 release draft.' }
$release = $matchingReleases[0]
if (!$release.draft) { throw 'Only an unpublished draft may be repackaged.' }
$root = Join-Path $env:RUNNER_TEMP 'sweetcherry-notices'
$inputDir = Join-Path $root 'original'
$outputDir = Join-Path $root 'updated'
$stage = Join-Path $root 'stage'
New-Item $inputDir,$outputDir,$stage -ItemType Directory -Force | Out-Null
$zipName = "SweetCherry-$version-windows-x64.zip"
$exeName = "SweetCherry-$version-windows-x64-setup.exe"
foreach ($name in @($zipName,$exeName,'SHA256SUMS.txt','windows-bundle.json')) {
    & gh release download $tag --repo $repo --pattern $name --dir $inputDir
    if ($LASTEXITCODE -ne 0) { throw "Cannot download draft asset: $name" }
}
foreach ($name in @($zipName,$exeName)) {
    $line = @(Get-Content "$inputDir/SHA256SUMS.txt" | Where-Object { $_ -match "^[a-fA-F0-9]{64}\s+$([regex]::Escape($name))$" })
    if ($line.Count -ne 1) { throw "Missing/ambiguous checksum: $name" }
    $expected = ($line[0] -split '\s+')[0]
    if ((Get-FileHash "$inputDir/$name" -Algorithm SHA256).Hash -ne $expected) { throw "Original checksum mismatch: $name" }
}
Expand-Archive "$inputDir/$zipName" $stage
$bundle = Join-Path $stage 'SweetCherry'
$manifest = Get-Content "$bundle/windows-bundle.json" -Raw | ConvertFrom-Json
if ($manifest.applicationVersion -ne $version -or $manifest.commit -ne $expectedCommit) { throw 'Unexpected original application build.' }
$beforeJar = (Get-FileHash "$bundle/SweetCherry.jar" -Algorithm SHA256).Hash
$beforeLauncher = (Get-FileHash "$bundle/SweetCherry.exe" -Algorithm SHA256).Hash
$noticeRoot = Join-Path $PSScriptRoot '../../distribution-notices'
if (!(Test-Path "$noticeRoot/THIRD-PARTY-NOTICES.md")) { throw 'Missing checked-in notices.' }
Copy-Item "$noticeRoot/THIRD-PARTY-NOTICES.md" $bundle
Copy-Item "$noticeRoot/licenses" $bundle -Recurse
$manifest | Add-Member -NotePropertyName noticesCommit -NotePropertyValue $env:GITHUB_SHA -Force
$manifest | ConvertTo-Json | Set-Content "$bundle/windows-bundle.json" -Encoding utf8
Copy-Item "$bundle/windows-bundle.json" $outputDir
$iscc = 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe'
if (!(Test-Path $iscc)) { throw 'Inno Setup 6 is required on the Windows runner.' }
& $iscc "/DSourceDir=$bundle" "/DOutputDir=$outputDir" "/DAppVersion=$version" '/DFileVersion=1.0.0.0' "$PSScriptRoot/SweetCherry.iss"
if ($LASTEXITCODE -ne 0) { throw 'Installer repack failed.' }
Compress-Archive -Path $bundle -DestinationPath "$outputDir/$zipName"
Compress-Archive -Path "$noticeRoot/THIRD-PARTY-NOTICES.md","$noticeRoot/licenses" -DestinationPath "$outputDir/SweetCherry-1.0.0-notices.zip"
if ((Get-FileHash "$bundle/SweetCherry.jar" -Algorithm SHA256).Hash -ne $beforeJar -or
    (Get-FileHash "$bundle/SweetCherry.exe" -Algorithm SHA256).Hash -ne $beforeLauncher) { throw 'Application binaries unexpectedly changed.' }
Get-ChildItem $outputDir -File | Where-Object { $_.Extension -in @('.exe','.zip') } | Sort-Object Name | ForEach-Object {
    "$((Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant())  $($_.Name)"
} | Set-Content "$outputDir/SHA256SUMS.txt" -Encoding ascii
"SC_ORIGINAL_ASSETS=$inputDir" >> $env:GITHUB_ENV
"SC_UPDATED_ASSETS=$outputDir" >> $env:GITHUB_ENV
"SC_DRAFT_RELEASE_ID=$($release.id)" >> $env:GITHUB_ENV
