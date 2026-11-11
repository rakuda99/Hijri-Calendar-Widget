import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# I need to refactor SettingsDialog.init_ui to use a QTabWidget.
# Right now it does:
# main_layout = QVBoxLayout(self)
# ...
# main_layout.addLayout(layout) -> wait, it actually does: layout = QVBoxLayout(self)
