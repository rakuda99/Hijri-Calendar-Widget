import re
with open("desktop_app/widget_ui.py", "r", encoding="utf-8") as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if "data[\"name\"].setStyleSheet(f\"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: \\'Tajawal\\';\")" in line:
        if line.startswith("                            "):
            lines[i] = line.replace("                            ", "                ")
            print(f"Fixed line {i+1}")

with open("desktop_app/widget_ui.py", "w", encoding="utf-8") as f:
    f.writelines(lines)
