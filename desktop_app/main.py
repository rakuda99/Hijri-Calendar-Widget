import sys
import os
from PyQt6.QtWidgets import QApplication
from PyQt6.QtGui import QIcon
from PyQt6.QtCore import Qt

try:
    from desktop_app.settings_manager import SettingsManager
except ImportError:
    from settings_manager import SettingsManager

try:
    from desktop_app import widget_ui
    AppManager = widget_ui.AppManager
    get_asset_path = widget_ui.get_asset_path
except ImportError:
    import widget_ui
    AppManager = widget_ui.AppManager
    get_asset_path = widget_ui.get_asset_path

def main():
    QApplication.setHighDpiScaleFactorRoundingPolicy(
        Qt.HighDpiScaleFactorRoundingPolicy.PassThrough
    )

    app = QApplication(sys.argv)
    app.setApplicationName("HijriCalendarWidget")
    app.setOrganizationName("HijriApp")

    icon_path = get_asset_path("app_icon.ico")
    if os.path.exists(icon_path):
        app.setWindowIcon(QIcon(icon_path))
    else:
        png_path = get_asset_path("app_icon.png")
        if os.path.exists(png_path):
            app.setWindowIcon(QIcon(png_path))

    app.setQuitOnLastWindowClosed(False)

    settings_mgr = SettingsManager()
    manager = AppManager(settings_mgr)
    manager.show_all()

    sys.exit(app.exec())

if __name__ == "__main__":
    main()
