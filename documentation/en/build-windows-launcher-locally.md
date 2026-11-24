# Build the SweetCherry Windows launcher locally

These instructions build `SweetCherry.exe` from an existing local SweetCherry source folder. No GitHub account, GitHub Actions workflow, push, tag, or release is required. The launcher-only procedure also works with an extracted source archive without a `.git` directory.

`SweetCherry.exe` is a Launch4j launcher for `SweetCherry.jar`. It does not contain the application JAR or Java. Keep it in the complete application folder, with either a Windows x64 Java 17+ runtime in the `runtime` subfolder or a system Java 17+ runtime. The installer EXE is a separate product built with Inno Setup.

## 1. Prepare the tools and source folder

Use Windows x64, PowerShell, and a Windows x64 **JDK 17**. JDK 17 matches the project's Java target and Windows distribution workflow. A JRE alone cannot compile the source. Node.js 24 is needed if you also run the JavaScript tests.

Use a fresh source/build folder, separate from an application folder containing your databases, credentials, and settings. `mvnw.cmd clean package` recreates `target`, but does not clean every previously generated file in `release` or `dist`.

Open PowerShell in the folder containing `pom.xml` and `mvnw.cmd`:

```powershell
Set-Location 'C:\Build\SweetCherry'
# Replace this example with your actual Windows x64 JDK 17 installation.
$env:JAVA_HOME = 'C:\Program Files\Microsoft\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

Both commands should report Java 17. Keep this PowerShell window open for the following steps.

## 2. Build the Java application

```powershell
.\mvnw.cmd --batch-mode clean package
if ($LASTEXITCODE -ne 0) { throw 'Application build failed.' }
```

This runs the Maven tests and prepares `release\SweetCherry`, including `SweetCherry.jar`, launch scripts, sample databases, and license files. It **does not** build the Windows launcher by itself.

You can run the JavaScript tests as an additional check:

```powershell
node --test src/test/js/*.test.cjs
if ($LASTEXITCODE -ne 0) { throw 'JavaScript tests failed.' }
```

## 3. Generate the local Launch4j configuration

The `windows-launcher` Maven profile reads `dist\windows\launch4j.xml`. Generate that file from the checked-in template, using absolute output and icon paths:

```powershell
$repo = (Get-Location).Path
$bundle = Join-Path $repo 'release\SweetCherry'
$out = Join-Path $repo 'dist\windows'
if (!(Test-Path (Join-Path $bundle 'SweetCherry.jar'))) {
    throw 'Run Maven package successfully first.'
}
New-Item $out -ItemType Directory -Force | Out-Null
[xml]$config = Get-Content (Join-Path $repo 'packaging\windows\launch4j.xml') -Raw -Encoding UTF8
$config.launch4jConfig.outfile = Join-Path $bundle 'SweetCherry.exe'
$config.launch4jConfig.icon = Join-Path $repo 'packaging\windows\SweetCherry.ico'
# Version information is required by the Maven Launch4j plugin.
[xml]$pom = Get-Content (Join-Path $repo 'pom.xml') -Raw -Encoding utf8
$version = [string]$pom.project.version
if ($version -notmatch '^(\d+)\.(\d+)\.(\d+)(?:-[A-Za-z0-9.-]+)?$') { throw 'Unsupported project version' }
$fileVersion = "$($Matches[1]).$($Matches[2]).$($Matches[3]).0"
$versionInfo = $config.CreateElement('versionInfo')
foreach ($entry in ([ordered]@{
    fileVersion=$fileVersion; txtFileVersion=$version; fileDescription='SweetCherry Windows x64 launcher';
    copyright='2024 Turker Ozturk'; productVersion=$fileVersion; txtProductVersion=$version;
    productName='SweetCherry'; internalName='SweetCherry'; originalFilename='SweetCherry.exe'
}).GetEnumerator()) {
    $node=$config.CreateElement($entry.Key); $node.InnerText=$entry.Value; [void]$versionInfo.AppendChild($node)
}
[void]$config.launch4jConfig.AppendChild($versionInfo)
$config.Save((Join-Path $out 'launch4j.xml'))
```

The relative `SweetCherry.jar` and `runtime` paths in the template are intentional: they describe the application layout when the launcher runs. Do not change them to your build computer's absolute paths.

## 4. Compile the launcher

```powershell
.\mvnw.cmd --batch-mode -Pwindows-launcher com.akathist.maven.plugins.launch4j:launch4j-maven-plugin:2.7.0:launch4j
if ($LASTEXITCODE -ne 0) { throw 'Launcher build failed.' }
Get-Item (Join-Path $bundle 'SweetCherry.exe')
```

The output is `release\SweetCherry\SweetCherry.exe`. This goal builds only the launcher; it does not rerun the application build or tests. The plugin resolves the Launch4j tools through Maven. You do not need a separate Launch4j GUI installation, a C/C++ compiler, Inno Setup, or Git for this launcher-only procedure. This minimal launcher uses the template's icon and startup settings; the full bundle script additionally generates Windows version metadata.

## 5. Supply Java and the external configuration

The launcher first searches the adjacent `runtime` folder, then `JAVA_HOME` and `PATH`, then the Windows Java registry. Only 64-bit Java 17 or newer is accepted. You can reuse a complete Windows x64 Java 17 runtime from an existing SweetCherry distribution, or extract a trusted Windows x64 Java 17 JRE ZIP and copy the **contents of its top-level Java folder** into `runtime`.

Alternatively, for your own local use, copy the entire JDK 17 already used above. This is larger than a JRE, but requires no additional runtime download:

```powershell
$runtime = Join-Path $bundle 'runtime'
if (Test-Path $runtime) { throw 'A runtime already exists; inspect it before continuing.' }
New-Item $runtime -ItemType Directory | Out-Null
Copy-Item -Path (Join-Path $env:JAVA_HOME '*') -Destination $runtime -Recurse
& (Join-Path $runtime 'bin\java.exe') -version
if ($LASTEXITCODE -ne 0) { throw 'Bundled Java could not start.' }

$configFile = Join-Path $bundle 'application.yml'
if (!(Test-Path $configFile)) {
    Copy-Item (Join-Path $repo 'target\classes\application.yml') $configFile
}
```

Keep the Java `legal` and license files when copying a runtime. The required layout is:

```text
release\SweetCherry\SweetCherry.exe
release\SweetCherry\SweetCherry.jar
release\SweetCherry\application.yml
release\SweetCherry\runtime\bin\java.exe
```

Keep the other generated distribution files and subfolders alongside these files as well.

## 6. Test the launcher

Stop any SweetCherry instance already using the configured ports, then run:

```powershell
Start-Process -FilePath (Join-Path $bundle 'SweetCherry.exe') -WorkingDirectory $bundle
```

Check that the desktop control window opens, the server becomes ready, and the browser opens the application. Test opening a sample database and stopping the application from the control window. If startup fails, inspect `myapp.log` and use `run.bat` from the application folder for console diagnostics.

The launcher is Windows-specific. The Java-free portable ZIP uses the installed system Java through `run.bat`, `run.sh`, or `run.command`; its EXE can also use a compatible installed system Java.

## Optional: build the complete installer and release assets

Use a **separate fresh build folder** for this route. Do not run it after manually installing a runtime in step 5: the bundle script refuses an existing `runtime` folder and an already-used distribution.

Install Inno Setup 6.3 or newer and Git. Supply a previously obtained Temurin Windows x64 Java 17 **JRE ZIP**, its publisher-provided SHA-256, and its actual release name. The ZIP must contain one top-level Java folder with `bin\java.exe`. This route needs a local Git checkout because the script records `git rev-parse HEAD` in `build-info.json`; no GitHub connection is needed for that command.

After the normal Maven build, run the existing script with your actual values:

```powershell
.\packaging\windows\build-bundle.ps1 `
    -IsccPath 'C:\Program Files (x86)\Inno Setup 6\ISCC.exe' `
    -RuntimeArchive 'C:\BuildTools\temurin-jre17-windows-x64.zip' `
    -RuntimeSha256 'REPLACE_WITH_THE_PUBLISHER_SHA256' `
    -RuntimeVersion 'REPLACE_WITH_THE_ACTUAL_TEMURIN_RELEASE_NAME'
```

The checksum and release name above are placeholders, not runnable values. Obtain the expected checksum from the publisher; calculating a checksum of the ZIP alone does not verify its origin.

The script generates the launcher, version metadata, Java-containing installer, Java-free portable ZIP, third-party license ZIP, and `sha256sums.txt`. Public assets are written to `dist\windows`; filenames use the version in `pom.xml`. Nothing is published automatically.

## Building without any network access

Local building and offline building are different. On first use, Maven Wrapper and Maven may download Maven itself, application dependencies, build plugins, and Launch4j's platform tools. Some build steps may also need cached license data. Having the source code alone is not sufficient for an offline build.

Before disconnecting, complete the actual `clean package` and launcher commands once on the same Windows machine, with the same source version and Maven configuration. Retain the wrapper's Maven distribution cache, the complete Maven local repository, and any build-time data caches. Keep your JDK/runtime and, if needed, the Inno Setup installer locally.

With those prerequisites cached, use Maven's offline flag:

```powershell
.\mvnw.cmd --offline --batch-mode clean package
if ($LASTEXITCODE -ne 0) { throw 'Offline application build failed; check missing cached artifacts.' }
# Generate dist\windows\launch4j.xml as in step 3, then:
.\mvnw.cmd --offline --batch-mode -Pwindows-launcher com.akathist.maven.plugins.launch4j:launch4j-maven-plugin:2.7.0:launch4j
if ($LASTEXITCODE -ne 0) { throw 'Offline launcher build failed; check missing cached tools.' }
```

The wrapper itself must already have Maven cached; `--offline` cannot supply a missing Maven distribution. Verify a disconnected build before relying on it, since individual plugins may perform their own network operations. A missing cached artifact must be supplied beforehand; skipping tests does not solve missing build tools or dependencies.
