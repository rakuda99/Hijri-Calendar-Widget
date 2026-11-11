import re

file_path = r"desktop_app\widget_ui.py"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Hide AM/PM label
content = content.replace(
    "ampm_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)",
    "ampm_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)\n            ampm_lbl.hide()"
)

# 2. Add Autostart toggle to settings
content = content.replace(
    'self.chk_temp = add_toggle("Show Temperature / عرض درجة الحرارة", "show_temp", True)',
    'self.chk_temp = add_toggle("Show Temperature / عرض درجة الحرارة", "show_temp", True)\n        self.chk_autostart = add_toggle("Auto-Start / تشغيل تلقائي مع بدء الويندوز", "autostart", False)'
)

content = content.replace(
    'self.settings_mgr.set("show_temp", self.chk_temp.isChecked())',
    'self.settings_mgr.set("show_temp", self.chk_temp.isChecked())\n        self.settings_mgr.set("autostart", self.chk_autostart.isChecked())\n        self._apply_autostart(self.chk_autostart.isChecked())'
)

# Add the registry logic method to SettingsDialog
registry_logic = """
    def _apply_autostart(self, enable):
        import winreg, sys, os
        key_path = r"Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        app_name = "HijriCalendarWidget"
        try:
            registry_key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, key_path, 0, winreg.KEY_WRITE)
            if enable:
                exe_path = sys.executable if getattr(sys, 'frozen', False) else os.path.abspath(sys.argv[0])
                winreg.SetValueEx(registry_key, app_name, 0, winreg.REG_SZ, f'"{exe_path}"')
            else:
                try: winreg.DeleteValue(registry_key, app_name)
                except FileNotFoundError: pass
            winreg.CloseKey(registry_key)
        except Exception as e:
            print("Autostart error:", e)

"""
content = content.replace('def save_and_close(self):', registry_logic + '    def save_and_close(self):')

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied successfully.")
