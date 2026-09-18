@echo off
setlocal
cd /d "%~dp0"
if "%~1"=="" (
  wscript.exe "%~dp0Testserver.vbs"
) else (
  py -3 "%~dp0tools\test-servers\lab.py" %*
)
exit /b %errorlevel%
