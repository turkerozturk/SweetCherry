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

echo Java bulundu: %JAVA_VERSION%
echo SweetCherry derleniyor. Ilk derleme internet hizina gore birkac dakika surebilir...
call mvnw.cmd -DskipTests package
if errorlevel 1 goto builderror

echo.
echo DERLEME TAMAMLANDI: target\SweetCherry.jar
if /i not "%~1"=="--no-pause" pause
exit /b 0

:nojava
echo HATA: Java bulunamadi. JDK 17 veya daha yeni bir JDK kurun.
echo Ayrintilar: documentation\tr\setup-and-run.md
goto error

:badversion
echo HATA: Java surumu okunamadi. "java -version" komutunu kontrol edin.
goto error

:oldjava
echo HATA: Java %JAVA_VERSION% bulundu; SweetCherry icin JDK 17 veya daha yenisi gerekir.
goto error

:builderror
echo HATA: Derleme tamamlanamadi. Yukaridaki Maven hata mesajini inceleyin.

:error
if /i not "%~1"=="--no-pause" pause
exit /b 1

