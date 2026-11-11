import sys
from PyQt6.QtWidgets import QApplication
from desktop_app.widget_ui import SettingsDialog
from desktop_app.settings_manager import SettingsManager

app = QApplication(sys.argv)
mgr = SettingsManager()
dlg = SettingsDialog(mgr)
print("Settings dialog created successfully")
