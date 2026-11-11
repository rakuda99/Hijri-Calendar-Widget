@echo off
echo ════════════════════════════════════════════════════════
echo   All-in-One: Build + Install Lock Screen Widget
echo   كل-في-واحد: بناء + تثبيت ودجت شاشة القفل
echo ════════════════════════════════════════════════════════
echo.
echo This will:
echo   1. Build new APK with Lock Screen feature
echo   2. Install it on your phone automatically
echo.
echo سيتم:
echo   1. بناء APK جديد مع ميزة شاشة القفل
echo   2. تثبيته على هاتفك تلقائياً
echo.
pause

echo.
echo ════════════════════════════════════════════════════════
echo   [Step 1/4] Cleaning old build...
echo   [الخطوة 1/4] تنظيف البناء القديم...
echo ════════════════════════════════════════════════════════
call gradle clean

echo.
echo ════════════════════════════════════════════════════════
echo   [Step 2/4] Building new APK...
echo   [الخطوة 2/4] بناء APK جديد...
echo   ⏱️  This may take 1-2 minutes...
echo ════════════════════════════════════════════════════════
call gradle assembleDebug
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERROR: Build failed!
    echo ❌ خطأ: فشل البناء!
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════
echo   [Step 3/4] Copying APK...
echo   [الخطوة 3/4] نسخ ملف APK...
echo ════════════════════════════════════════════════════════
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    copy /Y "app\build\outputs\apk\debug\app-debug.apk" "HijriWidget_V3_LockScreen.apk"
    echo ✅ APK created: HijriWidget_V3_LockScreen.apk
) else (
    echo ❌ ERROR: APK file not found!
    pause
    exit /b 1
)

echo.
echo ════════════════════════════════════════════════════════
echo   [Step 4/4] Installing on your phone...
echo   [الخطوة 4/4] التثبيت على هاتفك...
echo ════════════════════════════════════════════════════════
echo.
echo Please make sure:
echo   ✓ Phone is connected via USB
echo   ✓ USB Debugging is enabled
echo.
echo تأكد من:
echo   ✓ الهاتف موصول عبر USB
echo   ✓ USB Debugging مفعّل
echo.
pause

call adb devices
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERROR: ADB not found!
    echo.
    echo You can install manually:
    echo   1. Copy HijriWidget_V3_LockScreen.apk to your phone
    echo   2. Open it and install
    echo.
    pause
    exit /b 1
)

echo.
echo Installing APK...
call adb install -r "HijriWidget_V3_LockScreen.apk"

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ✅✅✅ SUCCESS! تم بنجاح! ✅✅✅
    echo ════════════════════════════════════════════════════════
    echo.
    echo Launching app...
    call adb shell am start -n com.example.hijricalendarwidget/.MainActivity
    
    echo.
    echo ════════════════════════════════════════════════════════
    echo   📱 Now on your phone:
    echo   📱 الآن على هاتفك:
    echo ════════════════════════════════════════════════════════
    echo.
    echo   You should see these buttons at the top:
    echo   ستجد هذه الأزرار في الأعلى:
    echo.
    echo   ┌────────────────────┬────────────────────┐
    echo   │ 🏠 الشاشة الرئيسية │  🔒 شاشة القفل    │
    echo   │   (Green button)   │  (Orange button)   │
    echo   └────────────────────┴────────────────────┘
    echo.
    echo   Next steps:
    echo   1. Press "🔒 شاشة القفل" (orange button)
    echo   2. Change settings
    echo   3. Press "Save"
    echo   4. Add widget to home screen
    echo.
    echo   الخطوات التالية:
    echo   1. اضغط على "🔒 شاشة القفل" (الزر البرتقالي)
    echo   2. غيّر الإعدادات
    echo   3. اضغط "حفظ"
    echo   4. أضف الودجت للشاشة الرئيسية
    echo.
) else (
    echo.
    echo ════════════════════════════════════════════════════════
    echo   ⚠️  Installation failed!
    echo   ⚠️  فشل التثبيت!
    echo ════════════════════════════════════════════════════════
    echo.
    echo Manual installation:
    echo   The APK file is ready: HijriWidget_V3_LockScreen.apk
    echo   Copy it to your phone and install manually
    echo.
    echo التثبيت اليدوي:
    echo   ملف APK جاهز: HijriWidget_V3_LockScreen.apk
    echo   انسخه لهاتفك وثبّته يدوياً
    echo.
)

echo.
pause
