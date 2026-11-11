@echo off
cd /d "%~dp0"
echo Checking for connected Android devices...
call adb devices
if %ERRORLEVEL% NEQ 0 (
    echo ADB is not in your PATH or not working.
    echo Please make sure Android platform-tools are installed and in PATH.
    pause
    exit /b
)

echo.
echo Installing app-debug.apk...
call adb install -r "app\build\outputs\apk\debug\app-debug.apk"

echo.
echo Launching the App...
call adb shell am start -n com.example.hijricalendarwidget/.MainActivity

echo.
echo Done.
pause
