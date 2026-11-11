[Setup]
AppName=تقويم الهجري ومواقيت الصلاة
AppVersion=1.0.0
DefaultDirName={userappdata}\HijriCalendar
DefaultGroupName=تقويم الهجري
OutputBaseFilename=HijriCalendar_Setup
Compression=lzma2
SolidCompression=yes
SetupIconFile=desktop_app\assets\app_icon.ico
UninstallDisplayIcon={app}\HijriCalendar.exe

[Tasks]
Name: "desktopicon"; Description: "إنشاء اختصار على سطح المكتب"; GroupDescription: "اختصارات:"
Name: "autostart"; Description: "تشغيل البرنامج تلقائياً عند بدء تشغيل ويندوز"; GroupDescription: "خيارات إضافية:"

[Files]
Source: "dist\HijriCalendar.exe"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\تقويم الهجري"; Filename: "{app}\HijriCalendar.exe"
Name: "{commondesktop}\تقويم الهجري"; Filename: "{app}\HijriCalendar.exe"; Tasks: desktopicon
Name: "{userstartup}\تقويم الهجري"; Filename: "{app}\HijriCalendar.exe"; Tasks: autostart

[Run]
Filename: "{app}\HijriCalendar.exe"; Description: "تشغيل التطبيق الآن"; Flags: nowait postinstall skipifsilent
