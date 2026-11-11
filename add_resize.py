import re
with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()
text = text.replace('self.setMinimumWidth(480)', 'self.resize(480, 600)\n        self.setMinimumWidth(480)')
with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
