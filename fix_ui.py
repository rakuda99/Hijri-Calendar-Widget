import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Remove setFixedSize from SettingsDialog
text = re.sub(r'self\.setFixedSize\(\d+,\s*\d+\)', 'self.setMinimumWidth(480)', text, count=1) # only the first one (SettingsDialog)

# 2. Add border: none; to name and time labels in init_ui
text = re.sub(
    r'name_lbl\.setStyleSheet\("font-size: 12px; font-weight: bold; color: #94a3b8;"\)',
    r'name_lbl.setStyleSheet("font-size: 12px; font-weight: bold; color: #94a3b8; border: none; background: transparent;")',
    text
)
text = re.sub(
    r'time_lbl\.setStyleSheet\("font-size: 13px; font-weight: bold; color: #f8fafc;"\)',
    r'time_lbl.setStyleSheet("font-size: 13px; font-weight: bold; color: #f8fafc; border: none; background: transparent;")',
    text
)

# 3. Add border: none; to name and time labels in update_prayer_times
text = re.sub(
    r'data\["name"\]\.setStyleSheet\("font-size: 12px; font-weight: bold; color: #38bdf8;"\)',
    r'data["name"].setStyleSheet("font-size: 12px; font-weight: bold; color: #38bdf8; border: none; background: transparent;")',
    text
)
text = re.sub(
    r'data\["name"\]\.setStyleSheet\("font-size: 12px; font-weight: bold; color: #94a3b8;"\)',
    r'data["name"].setStyleSheet("font-size: 12px; font-weight: bold; color: #94a3b8; border: none; background: transparent;")',
    text
)
text = re.sub(
    r'data\["time"\]\.setStyleSheet\(f"font-size: \{fs\}px; font-weight: bold; color: \{col\};"\)',
    r'data["time"].setStyleSheet(f"font-size: {fs}px; font-weight: bold; color: {col}; border: none; background: transparent;")',
    text
)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
