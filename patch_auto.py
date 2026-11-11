import re

file_path = r"desktop_app\widget_ui.py"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace inline autostart with the proper one
old_code = """    def _apply_autostart(self, enable):
        import winreg, sys, os
        key_path = r"Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        app_name = "HijriCalendarWidget"
        try:
            registry_key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, key_path, 0, winreg.KEY_WRITE)
            if enable:
                exe_path = sys.executable if getattr(sys, 'frozen', False) else os.path.abspath(sys.argv[0])
                winreg.SetValueEx(registry_key, app_name, 0, winreg.REG_SZ, f'"\\{exe_path}\\')
            else:
                try: winreg.DeleteValue(registry_key, app_name)
                except FileNotFoundError: pass
            winreg.CloseKey(registry_key)
        except Exception as e:
            print("Autostart error:", e)"""

# Instead of exact replacement which is fragile, let's just find and replace the imports and usage.
# We will inject `from desktop_app.auto_start import set_autostart, is_autostart_enabled` at the top.
if "from desktop_app.auto_start import" not in content:
    content = content.replace("from PyQt6.QtWidgets import (", "from desktop_app.auto_start import set_autostart, is_autostart_enabled\nfrom PyQt6.QtWidgets import (")

# Modify where chk_autostart is initialized to use is_autostart_enabled()
content = re.sub(
    r'self\.chk_autostart = add_toggle\("Auto-Start / تشغيل تلقائي مع بدء الويندوز", "autostart", False\)',
    r'self.chk_autostart = add_toggle("Auto-Start / تشغيل تلقائي مع بدء الويندوز", "autostart", is_autostart_enabled())',
    content
)

# And in save_and_close: replace self._apply_autostart(...) with set_autostart(...)
content = re.sub(
    r'self\._apply_autostart\(self\.chk_autostart\.isChecked\(\)\)',
    r'set_autostart(self.chk_autostart.isChecked())',
    content
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("widget_ui.py patched for auto_start")
