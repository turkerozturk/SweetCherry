@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found. Install JDK 17 or a newer JDK.
  pause
  exit /b 1
)

if not exist "SweetCherry.jar" (
  echo ERROR: SweetCherry.jar was not found next to this script.
  pause
  exit /b 1
)

echo Starting SweetCherry...
echo Browser address: http://localhost:8080
echo Press Ctrl+C in this window to stop the application.
java -jar "SweetCherry.jar" --server.port=8443 --server.http.port=8080 --myapp.openWebBrowserOnStartup=true %*

echo.
echo SweetCherry stopped.
pause
