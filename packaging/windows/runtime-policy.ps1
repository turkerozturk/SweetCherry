# Actual extracted runtime properties, not the archive filename or caller's label.
function Assert-BundleJavaRuntime([string]$RuntimeInfo) {
    if ($RuntimeInfo -notmatch 'os.arch\s*=\s*amd64' -or
        $RuntimeInfo -notmatch 'java.version\s*=\s*(17\.\d+\.\d+(?:\.\d+)?)(?=\s|$)') {
        throw 'The bundle must contain a stable Windows x64 Java 17 runtime.'
    }
    if ([version]$Matches[1] -lt [version]'17.0.21.1') {
        throw 'The bundle requires patched Java 17.0.21.1 or newer (CVE-2026-114938).'
    }
}
