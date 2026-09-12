@echo off
setlocal EnableExtensions DisableDelayedExpansion
title ViaForge 1.8.9 Forge Builder

rem Set VIAFORGE_NO_PAUSE=1 when this launcher is called by another script/CI job.
rem With no arguments, build the release JAR. Otherwise forward Gradle tasks/options.
set "SCRIPT_DIR=%~dp0"
set "BUILD_EXIT=1"
set "VIAFORGE_JAVA="
set "VIAFORGE_SUBST_DRIVE="
set "VIAFORGE_RUN_CLIENT="
if /i "%~1"=="runClient" set "VIAFORGE_RUN_CLIENT=1"
if defined VIAFORGE_RUN_CLIENT title ViaForge 1.8.9 Forge Client

echo.
echo ============================================================
if defined VIAFORGE_RUN_CLIENT (
    echo                    ViaForge Client Launcher
) else (
    echo                    ViaForge Client Builder
)
echo ============================================================
echo.

rem Prefer an explicitly configured JDK 21, then look in common JDK locations,
rem and finally use a Java 21 installation exposed through PATH.  This is kept
rem inline (rather than using CALLed labels) so the launcher always exits once.
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" (
        "%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
        if not errorlevel 1 set "VIAFORGE_JAVA=%JAVA_HOME%"
    )
)

if not defined VIAFORGE_JAVA (
    for /d %%D in ("C:\Program Files\Java\jdk-21*") do (
        if not defined VIAFORGE_JAVA if exist "%%~fD\bin\java.exe" (
            "%%~fD\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
            if not errorlevel 1 set "VIAFORGE_JAVA=%%~fD"
        )
    )
)

if not defined VIAFORGE_JAVA (
    for /d %%D in ("C:\Program Files\BellSoft\LibericaJDK-21*") do (
        if not defined VIAFORGE_JAVA if exist "%%~fD\bin\java.exe" (
            "%%~fD\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
            if not errorlevel 1 set "VIAFORGE_JAVA=%%~fD"
        )
    )
)

if not defined VIAFORGE_JAVA (
    for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
        if not defined VIAFORGE_JAVA if exist "%%~fD\bin\java.exe" (
            "%%~fD\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
            if not errorlevel 1 set "VIAFORGE_JAVA=%%~fD"
        )
    )
)

if not defined VIAFORGE_JAVA (
    for /d %%D in ("C:\Program Files\Microsoft\jdk-21*") do (
        if not defined VIAFORGE_JAVA if exist "%%~fD\bin\java.exe" (
            "%%~fD\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
            if not errorlevel 1 set "VIAFORGE_JAVA=%%~fD"
        )
    )
)

if not defined VIAFORGE_JAVA (
    for /d %%D in ("C:\Program Files\Zulu\zulu-21*") do (
        if not defined VIAFORGE_JAVA if exist "%%~fD\bin\java.exe" (
            "%%~fD\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
            if not errorlevel 1 set "VIAFORGE_JAVA=%%~fD"
        )
    )
)

if not defined VIAFORGE_JAVA (
    for /d %%D in ("%USERPROFILE%\.jdks\*") do (
        if not defined VIAFORGE_JAVA if exist "%%~fD\bin\javac.exe" (
            "%%~fD\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
            if not errorlevel 1 set "VIAFORGE_JAVA=%%~fD"
        )
    )
)

if not defined VIAFORGE_JAVA (
    for /f "usebackq delims=" %%J in (`where.exe java 2^>nul`) do (
        if not defined VIAFORGE_JAVA for %%H in ("%%~dpJ..") do (
            if exist "%%~fH\bin\java.exe" (
                "%%~fH\bin\java.exe" -version 2>&1 | findstr /r /c:"21\.[0-9][0-9]*" >nul
                if not errorlevel 1 set "VIAFORGE_JAVA=%%~fH"
            )
        )
    )
)

if not defined VIAFORGE_JAVA (
    echo [ERROR] A JDK 21 installation could not be found.
    echo         Install JDK 21 or set JAVA_HOME to its installation directory.
    echo         JAVA_HOME currently is: %JAVA_HOME%
    goto :finish
)

if not exist "%SCRIPT_DIR%gradlew.bat" (
    echo [ERROR] Gradle wrapper not found: "%SCRIPT_DIR%gradlew.bat"
    goto :finish
)

set "JAVA_HOME=%VIAFORGE_JAVA%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem Unimined opens its transformed Forge JAR through Java's ZIP filesystem.
rem The normal cache location under this project is longer than legacy Win32
rem ZIP paths permit, so use a short-lived drive mapping for Gradle's cache.
rem Keep the project path short for the remapper. Gradle's normal user cache
rem is used below so its provisioned Java toolchains remain discoverable.
for %%D in (V U T S R Q P O N M L K J I H G F E) do (
    if not defined VIAFORGE_SUBST_DRIVE if not exist "%%D:\" set "VIAFORGE_SUBST_DRIVE=%%D:"
)
if defined VIAFORGE_SUBST_DRIVE (
    subst %VIAFORGE_SUBST_DRIVE% "%SCRIPT_DIR:~0,-1%" >nul 2>&1
    if errorlevel 1 set "VIAFORGE_SUBST_DRIVE="
)
echo [ViaForge] Using JDK: %JAVA_HOME%
if defined VIAFORGE_RUN_CLIENT (
    echo [ViaForge] Building and starting Minecraft with ViaForge...
) else (
    echo [ViaForge] Starting Gradle build...
)
echo.

rem Run Gradle from the short mapped drive while keeping its cache in the
rem normal user location so auto-provisioned toolchains are available.
rem On JDK 21, TinyRemapper opens the just-created development JAR with the
rem ZIP filesystem; resolving the project through the original long Desktop
rem path can make that open fail with AccessDeniedException on Windows.
if defined VIAFORGE_SUBST_DRIVE (
    pushd %VIAFORGE_SUBST_DRIVE%\
) else (
    pushd "%SCRIPT_DIR%"
)
set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
if "%~1"=="" (
    call "%SCRIPT_DIR%gradlew.bat" build --no-daemon
) else (
    call "%SCRIPT_DIR%gradlew.bat" %* --no-daemon
)
set "BUILD_EXIT=%ERRORLEVEL%"
popd

if defined VIAFORGE_SUBST_DRIVE subst %VIAFORGE_SUBST_DRIVE% /D >nul 2>&1

if not "%BUILD_EXIT%"=="0" (
    echo.
    echo [ERROR] Gradle failed with exit code %BUILD_EXIT%.
    goto :finish
)

if defined VIAFORGE_RUN_CLIENT (
    echo.
    echo [ViaForge] Client task finished.
    goto :finish
)

echo.
echo [ViaForge] Build complete. Output files:
set "FOUND_ARTIFACT="
for %%F in ("%SCRIPT_DIR%build\libs\*.jar") do (
    if exist "%%~fF" (
        echo   %%~fF
        set "FOUND_ARTIFACT=1"
    )
)
if not defined FOUND_ARTIFACT echo   [WARNING] No JAR files were found in build\libs.
goto :finish

:finish
echo.
if not defined VIAFORGE_NO_PAUSE pause
endlocal & exit /b %BUILD_EXIT%
