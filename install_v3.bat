@echo off
echo ════════════════════════════════════════════════════════
echo   Installing Hijri Calendar Widget V3 (Lock Screen)
echo ════════════════════════════════════════════════════════
echo.

echo Checking for connected Android devices...
call adb devices
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ERROR: ADB is not working!
    echo ════════════════════════════════════════════════════════
    echo.
    echo Please make sure:
    echo   1. Android platform-tools are installed
    echo   2. ADB is in your PATH environment variable
    echo   3. Your phone is connected via USB
    echo   4. USB Debugging is enabled on your phone
    echo.
    pause
    exit /b 1
)

echo.
echo Installing the new APK with Lock Screen feature...
echo.

REM Try the new V3 APK first
if exist "HijriWidget_V3_LockScreen.apk" (
    echo Installing: HijriWidget_V3_LockScreen.apk
    call adb install -r "HijriWidget_V3_LockScreen.apk"
) else if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo Installing: app-debug.apk
    call adb install -r "app\build\outputs\apk\debug\app-debug.apk"
) else (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ERROR: No APK file found!
    echo ════════════════════════════════════════════════════════
    echo.
    echo Please run: build_new_apk.bat first to build the APK
    echo.
    pause
    exit /b 1
)

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ✅ Installation successful!
    echo ════════════════════════════════════════════════════════
    echo.
    echo Launching the app...
    call adb shell am start -n com.example.hijricalendarwidget/.MainActivity
    
    echo.
    echo ════════════════════════════════════════════════════════
    echo   📱 Now on your phone:
    echo ════════════════════════════════════════════════════════
    echo.
    echo   Look for these buttons at the top:
    echo   ┌────────────────────┬────────────────────┐
    echo   │ 🏠 الشاشة الرئيسية │  🔒 شاشة القفل    │
    echo   └────────────────────┴────────────────────┘
    echo.
    echo   1. Press "🔒 شاشة القفل" (orange button)
    echo   2. Change settings as you like
    echo   3. Press "Save"
    echo   4. Add widget to your home screen
    echo.
) else (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ⚠️  Installation failed!
    echo ════════════════════════════════════════════════════════
    echo.
    echo Possible reasons:
    echo   1. Phone is not connected properly
    echo   2. USB Debugging is not enabled
    echo   3. You need to allow installation on the phone
    echo.
)

echo.
pause
