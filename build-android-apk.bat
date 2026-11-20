@echo off
setlocal
title Blue Wave Laundry - Android APK Builder

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
set "ANDROID_HOME=C:\Users\arons\AppData\Local\Android\Sdk"
set "PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%"

echo ==================================================================
echo   Building Android Customer App APK (Blue Wave Laundry)
echo ==================================================================

cd /d "%~dp0android-customer-app"

echo Syncing latest web assets into Android project...
xcopy /s /y /i "..\src\main\resources\web\*" "app\src\main\assets\web\" > nul

echo Assembling Debug APK with Gradle...
call gradlew.bat assembleDebug

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    copy /y "app\build\outputs\apk\debug\app-debug.apk" "..\BlueWave-Laundry-Customer.apk" > nul
    echo.
    echo ==================================================================
    echo [SUCCESS] APK compiled successfully!
    echo Location: %~dp0BlueWave-Laundry-Customer.apk
    echo ==================================================================
) else (
    echo [ERROR] Build failed. Please check Gradle logs above.
)

pause
