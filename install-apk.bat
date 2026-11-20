@echo off
setlocal
title Blue Wave Laundry - Install APK to Android Device

set "ANDROID_HOME=C:\Users\arons\AppData\Local\Android\Sdk"
set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"

echo ==================================================================
echo   Installing Blue Wave Laundry Customer App on Android Device
echo ==================================================================

if not exist "%~dp0BlueWave-Laundry-Customer.apk" (
    echo [INFO] APK not found in root. Building now...
    call "%~dp0build-android-apk.bat"
)

echo Checking for connected Android devices or emulators...
adb devices

echo Installing BlueWave-Laundry-Customer.apk...
adb install -r "%~dp0BlueWave-Laundry-Customer.apk"

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] Installed on Android device!
    echo Starting app...
    adb shell am start -n com.laundry.customer/.MainActivity
) else (
    echo.
    echo [NOTICE] Ensure USB debugging is enabled on your phone or an emulator is running.
)

pause
