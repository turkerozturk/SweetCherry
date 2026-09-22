@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo HATA: Java bulunamadi. JDK 17 veya daha yeni bir JDK kurun.
  pause
  exit /b 1
)

if not exist "target\SweetCherry.jar" (
  echo HATA: target\SweetCherry.jar bulunamadi. Once build.bat dosyasini calistirin.
  pause
  exit /b 1
)

echo SweetCherry baslatiliyor...
echo Tarayici adresi: http://localhost:8080
echo Durdurmak icin bu pencerede Ctrl+C tuslarina basin.
java -jar "target\SweetCherry.jar" --server.port=8443 --server.http.port=8080 --myapp.openWebBrowserOnStartup=true

echo.
echo SweetCherry durdu.
pause

