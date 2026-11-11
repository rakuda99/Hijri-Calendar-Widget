@echo off
chcp 65001 > nul
echo ========================================================
echo   جاري تجميع تطبيق ويدجت التقويم الهجري إلى ملف EXE...
echo ========================================================

cd /d "%~dp0\.."

python -m PyInstaller --name="HijriCalendarWidget" --onefile --windowed --icon="desktop_app\assets\app_icon.ico" --add-data="desktop_app\assets;assets" --hidden-import="hijridate" --hidden-import="hijri_converter" --hidden-import="PyQt6" --hidden-import="desktop_app.widget_ui" --hidden-import="desktop_app.settings_manager" --clean desktop_app\main.py

echo.
if %errorlevel% equ 0 (
    echo ========================================================
    echo   تم إنشاء ملف HijriCalendarWidget.exe بنجاح!
    echo   الملف موجود في مجلد: dist\HijriCalendarWidget.exe
    echo ========================================================
    copy "dist\HijriCalendarWidget.exe" "HijriCalendarWidget.exe" > nul
    echo   تم نسخ الملف إلى المجلد الرئيسي: HijriCalendarWidget.exe
) else (
    echo   حدث خطأ أثناء التجميع!
)
