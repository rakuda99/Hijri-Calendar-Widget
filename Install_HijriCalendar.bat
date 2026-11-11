@echo off
:: Hijri Calendar Installer
echo Installing Hijri Calendar...
set "INSTALL_DIR=%LOCALAPPDATA%\Programs\HijriCalendar"
mkdir "%INSTALL_DIR%" 2>nul
copy /y "HijriCalendarWidget.exe" "%INSTALL_DIR%\HijriCalendarWidget.exe"

echo Creating shortcuts...
powershell -Command "$WShell = New-Object -ComObject WScript.Shell; $Shortcut = $WShell.CreateShortcut('%USERPROFILE%\Desktop\Hijri Calendar.lnk'); $Shortcut.TargetPath = '%INSTALL_DIR%\HijriCalendarWidget.exe'; $Shortcut.WorkingDirectory = '%INSTALL_DIR%'; $Shortcut.Save()"

powershell -Command "$WShell = New-Object -ComObject WScript.Shell; $Shortcut = $WShell.CreateShortcut('%APPDATA%\Microsoft\Windows\Start Menu\Programs\Hijri Calendar.lnk'); $Shortcut.TargetPath = '%INSTALL_DIR%\HijriCalendarWidget.exe'; $Shortcut.WorkingDirectory = '%INSTALL_DIR%'; $Shortcut.Save()"

echo Installation Complete!
echo You can now run Hijri Calendar from your Desktop or Start Menu.
pause
