@echo off
setlocal
cd /d "%~dp0"

call build.bat --no-pause
if errorlevel 1 (
  pause
  exit /b 1
)

call run.bat %*
