@echo off
cd /d "%~dp0"
echo.
echo ==================================================
echo   HIJRI CALENDAR WIDGET - ARABIC UPDATE v2.0
echo ==================================================
echo.
echo 1. Cleaning...
call gradle clean > nul 2>&1

echo 2. Building APK (Version 2.0)...
call gradle assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo !!! BUILD FAILED !!!
    pause
    exit /b 1
)

set BUILD_NUM=%RANDOM%
copy /Y "app\build\outputs\apk\debug\Hijri*.apk" "HijriMonthCalendar_v2.3_Build%BUILD_NUM%.apk"
copy /Y "app\build\outputs\apk\debug\Hijri*.apk" "HijriMonthCalendar.apk"
echo *** NEW APK GENERATED: HijriMonthCalendar_v2.3_Build%BUILD_NUM%.apk ***
echo.

echo.
echo 3. Installing...
echo    Make sure phone is connected.
echo.

call adb install -r "HijriMonthCalendar.apk"
echo.
echo =================================================
echo   DONE. ARABIC LANGUAGE SUPPORT ADDED.
echo   Check if Day Names are now in Arabic (if selected).
echo =================================================
pause
