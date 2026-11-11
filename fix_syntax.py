with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    skip = False
    for i, line in enumerate(lines):
        if 'else:' in line and 'data["frame"].setStyleSheet(f"""' in lines[i+2]:
            skip = True
        if skip:
            if '# Countdown string' in line:
                skip = False
                f.write(line)
        else:
            f.write(line)
