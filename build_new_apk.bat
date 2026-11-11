@echo off
cd /d "%~dp0"
echo ════════════════════════════════════════════════════════
echo   Building Hijri Calendar Widget with Lock Screen Feature
echo ════════════════════════════════════════════════════════
echo.

echo [1/3] Cleaning old build files...
call gradle clean
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Clean failed!
    echo Trying to continue anyway...
)

echo.
echo [2/3] Building new APK with Lock Screen feature...
call gradle assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ERROR: Build failed!
    echo ════════════════════════════════════════════════════════
    echo.
    echo Please make sure:
    echo   1. Android SDK is installed
    echo   2. ANDROID_HOME environment variable is set
    echo   3. Gradle is installed and in PATH
    echo.
    pause
    exit /b 1
)

echo.
echo [3/3] Locating and copying APK...
set "APK_FOUND="
for /R "app\build\outputs\apk\debug" %%F in (*.apk) do (
    set "APK_FOUND=%%F"
    copy /Y "%%F" "HijriWidget_v5.0_Build50.apk"
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ✅ SUCCESS! APK built: HijriWidget_v5.0_Build50.apk
    echo ════════════════════════════════════════════════════════
    goto :found
)
if not defined APK_FOUND (
    echo ERROR: No APK file found in app\build\outputs\apk\debug!
)
:found

echo.
pause
