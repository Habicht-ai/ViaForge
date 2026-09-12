@echo off
setlocal EnableExtensions DisableDelayedExpansion
rem Uses the same Java detection and Windows path handling as the builder.
call "%~dp0build.bat" runClient %*
endlocal & exit /b %ERRORLEVEL%
