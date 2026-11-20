@echo off
setlocal
title Blue Wave Laundry Unified Server Engine (Customer & Admin Portals)

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
set "MAVEN_HOME=C:\Users\arons\.gemini\antigravity\scratch\maven\apache-maven-3.9.6"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo ==================================================================
echo   Starting Laundry Unified Server (Customer App + Admin Portal)
echo ==================================================================

if exist "target\laundry-management-system-1.0.0.jar" (
    echo Launching pre-built server package...
    java -jar target\laundry-management-system-1.0.0.jar --server
) else (
    echo Building and launching server via Maven...
    mvn compile exec:java -Dexec.mainClass="com.laundry.app.Main" -Dexec.args="--server"
)

pause
