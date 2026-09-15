@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title AI Copywriter Backend

set "BACKEND_ROOT=%~dp0"
if "%BACKEND_ROOT:~-1%"=="\" set "BACKEND_ROOT=%BACKEND_ROOT:~0,-1%"

call :LoadEnv JAVA_HOME
call :LoadEnv JRE_HOME
call :LoadEnv CATALINA_HOME
call :LoadEnv CATALINA_BASE
call :LoadEnv CATALINA_TMPDIR

if not defined JRE_HOME set "JRE_HOME=%JAVA_HOME%"
if not defined CATALINA_BASE set "CATALINA_BASE=%CATALINA_HOME%"
if not defined CATALINA_TMPDIR set "CATALINA_TMPDIR=%CATALINA_BASE%\temp"
set "PATH=%JAVA_HOME%\bin;%PATH%"

set "APP_NAME=moment-copywriter"
set "DEPLOY_DIR=%CATALINA_HOME%\webapps\%APP_NAME%"
set "CHECK_ONLY="
set "NO_PAUSE="
if /I "%~1"=="--check" (
    set "CHECK_ONLY=1"
    set "DEPLOY_DIR=%TEMP%\moment-copywriter-backend-check-%RANDOM%-%RANDOM%"
)
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"
if /I "%~2"=="--no-pause" set "NO_PAUSE=1"

call :LoadEnv MOMENT_DB_URL
call :LoadEnv MOMENT_DB_USER
call :LoadEnv MOMENT_DB_PASSWORD
call :LoadEnv AI_API_URL
call :LoadEnv AI_API_KEY
call :LoadEnv AI_MODEL
call :LoadEnv BACKEND_HEALTH_URL

echo.
echo [1/5] Checking local environment...

if not defined JAVA_HOME (
    echo ERROR: Missing environment variable JAVA_HOME.
    goto :Fail
)

if not defined CATALINA_HOME (
    echo ERROR: Missing environment variable CATALINA_HOME.
    goto :Fail
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo ERROR: javac.exe not found: "%JAVA_HOME%\bin\javac.exe"
    goto :Fail
)

if not exist "%CATALINA_HOME%\bin\startup.bat" (
    echo ERROR: Tomcat startup.bat not found: "%CATALINA_HOME%\bin\startup.bat"
    goto :Fail
)

if not exist "%CATALINA_HOME%\lib\servlet-api.jar" (
    echo ERROR: servlet-api.jar not found: "%CATALINA_HOME%\lib\servlet-api.jar"
    goto :Fail
)

if not exist "%BACKEND_ROOT%\src\web\WEB-INF\lib\gson-2.13.1.jar" (
    echo ERROR: gson jar not found in WEB-INF\lib.
    goto :Fail
)

if not exist "%BACKEND_ROOT%\src\web\WEB-INF\lib\mssql-jdbc-13.2.1.jre11.jar" (
    echo ERROR: SQL Server JDBC jar not found in WEB-INF\lib.
    goto :Fail
)

if not defined MOMENT_DB_PASSWORD (
    echo ERROR: Missing environment variable MOMENT_DB_PASSWORD.
    goto :Fail
)

if not defined AI_API_KEY (
    echo ERROR: Missing environment variable AI_API_KEY.
    goto :Fail
)

if not defined AI_MODEL (
    echo ERROR: Missing environment variable AI_MODEL.
    goto :Fail
)

echo [2/5] Preparing deployment directory...
if defined CHECK_ONLY (
    echo Check mode: compiling into a temporary directory only.
) else (
    call "%CATALINA_HOME%\bin\shutdown.bat" >nul 2>nul
    timeout /t 2 /nobreak >nul
)

if exist "%DEPLOY_DIR%" (
    rmdir /s /q "%DEPLOY_DIR%"
    if errorlevel 1 (
        echo ERROR: Failed to remove deployment directory: "%DEPLOY_DIR%"
        echo Close running Tomcat windows and try again.
        goto :Fail
    )
)

mkdir "%DEPLOY_DIR%" >nul 2>nul
mkdir "%DEPLOY_DIR%\WEB-INF\classes" >nul 2>nul

echo [3/5] Copying web files...
xcopy "%BACKEND_ROOT%\src\web\*" "%DEPLOY_DIR%\" /E /I /Y >nul
if errorlevel 1 (
    echo ERROR: Failed to copy web files.
    goto :Fail
)

echo [4/5] Compiling Java sources...
set "SOURCE_LIST=%TEMP%\moment-copywriter-sources-%RANDOM%-%RANDOM%.txt"
if exist "%SOURCE_LIST%" del /q "%SOURCE_LIST%" >nul 2>nul

for /r "%BACKEND_ROOT%\src" %%F in (*.java) do (
    echo %%~fF | findstr /I "\\src\\web\\" >nul
    if errorlevel 1 (
        set "SOURCE_FILE=%%~fF"
        set "SOURCE_FILE=!SOURCE_FILE:\=/!"
        echo "!SOURCE_FILE!">>"%SOURCE_LIST%"
    )
)

set "CLASSPATH=%DEPLOY_DIR%\WEB-INF\lib\*;%CATALINA_HOME%\lib\servlet-api.jar"
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -cp "%CLASSPATH%" -d "%DEPLOY_DIR%\WEB-INF\classes" @"%SOURCE_LIST%"
set "JAVAC_EXIT=%ERRORLEVEL%"
del /q "%SOURCE_LIST%" >nul 2>nul

if not "%JAVAC_EXIT%"=="0" (
    echo ERROR: Java compilation failed.
    goto :Fail
)

if defined CHECK_ONLY (
    echo [5/5] Check completed. Backend sources compile successfully.
    echo.
    if not defined NO_PAUSE pause
    exit /b 0
)

echo [5/5] Starting Tomcat...
call "%CATALINA_HOME%\bin\startup.bat"
if errorlevel 1 (
    echo ERROR: Tomcat failed to start.
    goto :Fail
)

echo.
echo Backend is starting.
timeout /t 4 /nobreak >nul
if defined BACKEND_HEALTH_URL (
    start "" "%BACKEND_HEALTH_URL%"
) else (
    echo BACKEND_HEALTH_URL is not configured. Open the health check endpoint according to your deployment address.
)
echo.
echo If the browser shows 404 or connection refused, wait a few seconds and refresh.
echo.
if not defined NO_PAUSE pause
exit /b 0

:LoadEnv
set "ENV_VALUE="
for /f "tokens=2,*" %%A in ('reg query HKCU\Environment /v %~1 2^>nul') do (
    set "ENV_VALUE=%%B"
)
if not defined ENV_VALUE (
    for /f "tokens=2,*" %%A in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v %~1 2^>nul') do (
        set "ENV_VALUE=%%B"
    )
)
if defined ENV_VALUE set "%~1=%ENV_VALUE%"
exit /b 0

:Fail
echo.
echo Startup failed. Read the error above, then press any key to close this window.
if not defined NO_PAUSE pause >nul
exit /b 1
