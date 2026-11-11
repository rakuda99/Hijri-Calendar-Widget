@echo off
echo Building Month Calendar Widget...

call gradle assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    pause
    exit /b 1
)

echo Installing...
adb install -r app\build\outputs\apk\debug\app-debug.apk

echo Launching...
adb shell am start -n com.example.hijricalendarwidget/.MainActivity

echo Done.
