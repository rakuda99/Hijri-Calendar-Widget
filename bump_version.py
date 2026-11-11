import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('v3.0', 'v3.1')

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)

with open('HijriCalendarWidget.spec', 'r', encoding='utf-8') as f:
    spec_text = f.read()
    
spec_text = spec_text.replace('HijriCalendarWidget_v3.0', 'HijriCalendarWidget_v3.1')

with open('HijriCalendarWidget.spec', 'w', encoding='utf-8') as f:
    f.write(spec_text)

with open('version_info.txt', 'r', encoding='utf-8') as f:
    ver_text = f.read()
    
ver_text = ver_text.replace('3, 0, 0, 0', '3, 1, 0, 0').replace('3.0.0.0', '3.1.0.0').replace('_v3.0', '_v3.1')

with open('version_info.txt', 'w', encoding='utf-8') as f:
    f.write(ver_text)
