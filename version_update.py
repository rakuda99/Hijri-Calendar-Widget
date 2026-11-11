import re
with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('self.setWindowTitle("إعدادات التقويم الهجري ومواقيت الصلاة")', 'self.setWindowTitle("إعدادات التقويم الهجري ومواقيت الصلاة v3.0")')
text = text.replace('self.setWindowTitle("تقويم الشهر")', 'self.setWindowTitle("تقويم الشهر v3.0")')

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
