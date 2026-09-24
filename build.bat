@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 goto nojava

for /f "tokens=3" %%V in ('java -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~V"
for /f "tokens=1,2 delims=." %%A in ("%JAVA_VERSION%") do (
  set "JAVA_MAJOR=%%A"
  if "%%A"=="1" set "JAVA_MAJOR=%%B"
)
if not defined JAVA_MAJOR goto badversion
if %JAVA_MAJOR% LSS 17 goto oldjava

echo Java found: %JAVA_VERSION%
echo Building SweetCherry. The first build may take a few minutes depending on your internet connection...
call mvnw.cmd -DskipTests package
if errorlevel 1 goto builderror

echo.
echo BUILD COMPLETED: target\SweetCherry.jar
echo RUNNABLE DIRECTORY: release\SweetCherry\
if /i not "%~1"=="--no-pause" pause
exit /b 0

:nojava
echo ERROR: Java was not found. Install JDK 17 or a newer JDK.
echo Details: documentation\tr\setup-and-run.md
goto error

:badversion
echo ERROR: The Java version could not be read. Check the "java -version" command.
goto error

:oldjava
echo ERROR: Java %JAVA_VERSION% was found; SweetCherry requires JDK 17 or newer.
goto error

:builderror
echo ERROR: The build could not be completed. Review the Maven error above.

:error
if /i not "%~1"=="--no-pause" pause
exit /b 1
