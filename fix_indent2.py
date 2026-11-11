import re
with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    lines = f.readlines()

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    for line in lines:
        if line.startswith('                def populate_colors'):
            f.write(line.replace('                def', '        def'))
        elif line.startswith('            combo.clear()'):
            f.write(line.replace('            combo.clear()', '            combo.clear()'))
        else:
            f.write(line)
