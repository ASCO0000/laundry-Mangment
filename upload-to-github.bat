@echo off
setlocal
title Push Laundry Management System to GitHub

echo ==================================================================
echo   Upload Blue Wave Laundry Management System to GitHub
echo ==================================================================
echo.

cd /d "%~dp0"

:: Check Git
where git >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Git is not found in your PATH. Please install Git for Windows.
    pause
    exit /b 1
)

:: Check if remote origin already exists
git remote get-url origin >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo An existing remote origin is already configured:
    git remote get-url origin
    echo.
    set /p PUSH_NOW="Do you want to push to this repository now? (Y/N): "
    if /i "%PUSH_NOW%"=="Y" (
        echo.
        echo Pushing to GitHub...
        git push -u origin main
        goto done
    )
)

echo.
echo Step 1: Create a new empty repository on GitHub (https://github.com/new).
echo         (Do NOT initialize with README, .gitignore, or license).
echo.
set /p REPO_URL="Enter your GitHub Repository URL (e.g. https://github.com/username/laundry-app.git): "

if "%REPO_URL%"=="" (
    echo [ERROR] No URL entered. Aborting.
    pause
    exit /b 1
)

:: Set remote origin
git remote remove origin >nul 2>&1
git remote add origin %REPO_URL%

echo.
echo Step 2: Uploading all files to branch 'main'...
git push -u origin main

:done
echo.
if %ERRORLEVEL% equ 0 (
    echo ==================================================================
    echo [SUCCESS] Your repository has been uploaded to GitHub!
    echo ==================================================================
) else (
    echo ==================================================================
    echo [NOTE] If GitHub asked you to sign in, complete the browser login prompt.
    echo ==================================================================
)

pause
