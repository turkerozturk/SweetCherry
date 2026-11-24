@echo off
setlocal
cd /d "%~dp0"

set "SWEETCHERRY_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "SWEETCHERRY_JAVA=%JAVA_HOME%\bin\java.exe"
if exist "runtime\bin\java.exe" set "SWEETCHERRY_JAVA=%~dp0runtime\bin\java.exe"

if not exist "runtime\bin\java.exe" (
  "%SWEETCHERRY_JAVA%" -version >nul 2>nul
  if errorlevel 1 (
    echo ERROR: Java was not found. Install JDK 17 or a newer JDK.
    pause
    exit /b 1
  )
)

if not exist "SweetCherry.jar" (
  echo ERROR: SweetCherry.jar was not found next to this script.
  pause
  exit /b 1
)

echo Starting SweetCherry...
echo Browser address and ports are read from application.yml.
echo Press Ctrl+C in this window to stop the application.
"%SWEETCHERRY_JAVA%" -jar "SweetCherry.jar" --myapp.openWebBrowserOnStartup=true %*

echo.
echo SweetCherry stopped.
pause
