import re

file_path = "desktop_app/widget_ui.py"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add ctypes imports at the top
if "import ctypes" not in content:
    content = content.replace("from PyQt6.QtWidgets import (", "import ctypes\nfrom ctypes import wintypes\n\ndef send_to_desktop_bottom(hwnd):\n    HWND_BOTTOM = 1\n    SWP_NOMOVE = 0x0002\n    SWP_NOSIZE = 0x0001\n    SWP_NOACTIVATE = 0x0010\n    ctypes.windll.user32.SetWindowPos(\n        int(hwnd), HWND_BOTTOM, 0, 0, 0, 0,\n        SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE\n    )\n\nfrom PyQt6.QtWidgets import (")

# Modify flags in both places
content = content.replace(
    "flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow",
    "flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint\n        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)"
)
content = content.replace(
    "self.setWindowFlags(flags)",
    "self.setWindowFlags(flags)\n        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)"
)

# Inject showEvent into ClockWidget if not there
if "def showEvent(self, event):" not in content:
    # Inject it before mousePressEvent in ClockWidget
    content = content.replace(
        "    def mousePressEvent(self, event):",
        "    def showEvent(self, event):\n        super().showEvent(event)\n        send_to_desktop_bottom(self.winId())\n\n    def mousePressEvent(self, event):"
    )

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Bottom patch applied")
