@echo off
setlocal
set "ROOT=%~dp0"
set "MAVEN_CMD="

where npm.cmd >nul 2>nul
if errorlevel 1 (
    echo ERROR: Node.js and npm are required and must be on PATH.
    exit /b 1
)

if not defined JAVA_HOME (
    for /d %%D in ("%ProgramFiles%\Java\jdk-*") do if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
)
if not defined JAVA_HOME (
    for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-*") do if exist "%%~fD\bin\javac.exe" set "JAVA_HOME=%%~fD"
)
if not defined JAVA_HOME (
    echo ERROR: Set JAVA_HOME to a Java 17 or newer JDK.
    exit /b 1
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo ERROR: JAVA_HOME does not point to a JDK: %JAVA_HOME%
    exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

where mvn.cmd >nul 2>nul
if not errorlevel 1 set "MAVEN_CMD=mvn.cmd"
if not defined MAVEN_CMD (
    for /d %%D in ("%USERPROFILE%\.maven\maven-*") do if exist "%%~fD\bin\mvn.cmd" set "MAVEN_CMD=%%~fD\bin\mvn.cmd"
)
if not defined MAVEN_CMD (
    echo ERROR: Maven 3.9 or newer must be on PATH or installed under %%USERPROFILE%%\.maven.
    exit /b 1
)

pushd "%ROOT%frontend"
call npm ci
if errorlevel 1 (
    popd
    echo ERROR: npm ci failed.
    exit /b 1
)
popd

start "Market Lane API" /D "%ROOT%backend" "%MAVEN_CMD%" spring-boot:run
start "Market Lane UI" /D "%ROOT%frontend" npm.cmd run dev -- --host 127.0.0.1
echo.
echo Market Lane is starting.
echo UI:  http://127.0.0.1:5173
echo API: http://localhost:8080/api
endlocal
