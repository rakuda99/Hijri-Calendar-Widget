import re

file_path = "desktop_app/widget_ui.py"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix the AppManager flags logic and remove setAttribute from AppManager
content = content.replace(
"""        if dlg.exec():
            stay_on_top = self.settings_mgr.get("stay_on_top", False)
            flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)""",
"""        if dlg.exec():
            stay_on_top = self.settings_mgr.get("stay_on_top", False)
            flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint"""
)

# And earlier in AppManager.__init__:
content = content.replace(
"""        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
        if stay_on_top:
            flags |= Qt.WindowType.WindowStaysOnTopHint""",
"""        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint
        if stay_on_top:
            flags |= Qt.WindowType.WindowStaysOnTopHint"""
)

# We need to set the attribute on the widgets, not the AppManager.
content = content.replace(
"""            self.clock_widget.setWindowFlags(flags)
            self.prayer_widget.setWindowFlags(flags)""",
"""            self.clock_widget.setWindowFlags(flags)
            self.prayer_widget.setWindowFlags(flags)
            self.clock_widget.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
            self.prayer_widget.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)"""
)

# And in PrayerWidget / ClockWidget __init__:
# Wait, ClockWidget __init__:
# self.setWindowFlags(self.windowFlags() & ~Qt.WindowType.WindowContextHelpButtonHint)
content = content.replace(
"""        self.setWindowFlags(self.windowFlags() & ~Qt.WindowType.WindowContextHelpButtonHint)
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)""",
"""        self.setWindowFlags(self.windowFlags() & ~Qt.WindowType.WindowContextHelpButtonHint)
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)"""
)

# Actually, if I look at my previous replacement:
# content = content.replace("self.setWindowFlags(flags)", "self.setWindowFlags(flags)\n        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)")
# Wait, this replaced EVERYTHING that matched `self.setWindowFlags(flags)`.
# But `AppManager` doesn't have `self.setWindowFlags(flags)`. 
# It sets it on `self.clock_widget.setWindowFlags(flags)`.

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Fixed AppManager indentation and attributes.")
