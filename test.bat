@echo off
setlocal
echo ==============================================================
echo Running Laundry Management System Automated Test Suite
echo ==============================================================

if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot" (
    set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    set "PATH=%JAVA_HOME%\bin;%PATH%"
)

if exist "%~dp0..\maven\apache-maven-3.9.6\bin" (
    set "PATH=%~dp0..\maven\apache-maven-3.9.6\bin;%PATH%"
)

mvn clean test

pause
