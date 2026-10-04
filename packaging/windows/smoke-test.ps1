[CmdletBinding()]
param([Parameter(Mandatory)][string]$InstallerPath)
$ErrorActionPreference = 'Stop'
# Exercise the actual installer, user-file preservation and launcher in a path with spaces/Unicode.
$testDir = Join-Path $env:RUNNER_TEMP 'SweetCherry test Türkçe'
New-Item "$testDir/CTBDATA","$testDir/allTenants" -ItemType Directory -Force | Out-Null
$sentinels = @{
    'CTBDATA/demo.ctb'='KEEP-CTB'; 'allTenants/demo.txt'='KEEP-TENANT';
    'application.yml'="server:`n  address: 127.0.0.1`nmyapp:`n  openWebBrowserOnStartup: false`n";
    'login-credentials.properties'="admin.password=ci-test-only`nuser.password=ci-test-only`n"
}
foreach ($entry in $sentinels.GetEnumerator()) { [IO.File]::WriteAllText((Join-Path $testDir $entry.Key),$entry.Value) }
$setup = Start-Process $InstallerPath -ArgumentList @('/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART',"/DIR=`"$testDir`"") -Wait -PassThru
if ($setup.ExitCode -ne 0) { throw "Installer failed: $($setup.ExitCode)" }
foreach ($entry in $sentinels.GetEnumerator()) {
    if ([IO.File]::ReadAllText((Join-Path $testDir $entry.Key)) -cne $entry.Value) { throw "Installer overwrote $($entry.Key)" }
}
# Remove deliberately invalid CTB fixture before startup; it was only an overwrite sentinel.
Remove-Item "$testDir/CTBDATA/demo.ctb","$testDir/allTenants/demo.txt"
# Empty PATH and JAVA_HOME in this child environment prove the EXE uses its bundled runtime.
$oldPath=$env:PATH; $oldJava=$env:JAVA_HOME
$process=$null
try {
    $env:PATH="$env:SystemRoot/System32"; $env:JAVA_HOME=''
    $process=Start-Process "$testDir/SweetCherry.exe" -ArgumentList @('--l4j-debug','--server.port=18443','--server.http.port=18080','--myapp.openWebBrowserOnStartup=false') -WorkingDirectory $env:RUNNER_TEMP -PassThru
    $ready=$false
    $lastResponse='No HTTP response received.'
    for ($attempt=0; $attempt -lt 60; $attempt++) {
        try {
            # No tenant is configured in this fixture: verify the real login UI, not database health.
            $result=Invoke-WebRequest 'http://127.0.0.1:18080/login' -TimeoutSec 2 -SkipHttpErrorCheck
            $lastResponse="Login HTTP $($result.StatusCode)"
            if ($result.StatusCode -eq 200 -and $result.Content -match 'name="username"' -and $result.Content -match 'name="password"') {
                $ready=$true
                break
            }
        } catch { $lastResponse=$_.Exception.Message }
        if ($process.HasExited) { throw "Launcher exited before the server became ready (exit code: $($process.ExitCode))." }
        Start-Sleep -Seconds 1
    }
    if (!$ready) { throw "Bundled launcher did not serve the login form: $lastResponse" }
    Write-Host 'Bundled launcher served the login form (HTTP 200).'
    try {
        $health=Invoke-WebRequest 'http://127.0.0.1:18080/actuator/health' -TimeoutSec 5 -SkipHttpErrorCheck
        Write-Host "Health diagnostics for the tenant-free fixture: HTTP $($health.StatusCode) $($health.Content)"
    } catch { Write-Host "Health diagnostics unavailable: $($_.Exception.Message)" }
    if (!(Test-Path "$testDir/myapp.log")) { throw 'Launcher did not set the application working directory.' }
} catch {
    Write-Host 'Bundled launcher startup diagnostics:'
    foreach ($log in @("$testDir/myapp.log", "$testDir/launch4j.log", "$env:RUNNER_TEMP/launch4j.log")) {
        if (Test-Path $log) {
            Write-Host "Log: $log"
            Get-Content $log -Encoding utf8 -Tail 120 | Write-Host
        }
    }
    throw
} finally {
    $env:PATH=$oldPath; $env:JAVA_HOME=$oldJava
    if ($process -and !$process.HasExited) { & "$env:SystemRoot/System32/taskkill.exe" /PID $process.Id /T /F | Out-Null }
}
Write-Host 'Installer preservation and bundled-runtime launcher smoke tests passed.'
