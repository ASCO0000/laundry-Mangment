@echo off
setlocal
echo ==============================================================
echo Starting Blue Wave Laundry Management System
echo ==============================================================

:: Detect or set JDK 17
if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

:: Detect Maven
if exist "%~dp0..\maven\apache-maven-3.9.6\bin" (
    set "PATH=%~dp0..\maven\apache-maven-3.9.6\bin;%PATH%"
)

echo Using Java:
java -version
echo.

if exist "target\laundry-management-system-1.0.0.jar" (
    echo Launching pre-built application package...
    java -jar target\laundry-management-system-1.0.0.jar
) else (
    echo Running Application via Maven...
    mvn compile exec:java -Dexec.mainClass="com.laundry.app.Main"
)

pause
