import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('        self.lbl_day.setVisible(show_clock)\n', '')
text = text.replace('        self.lbl_hijri_date.setVisible(show_clock)\n', '')
text = text.replace('        self.lbl_greg_date.setVisible(show_clock)\n', '')

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
