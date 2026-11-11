import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

clock_code = '''        
        show_clock = self.settings_mgr.get("show_clock", True)
        self.lbl_clock.setVisible(show_clock)
        self.lbl_ampm.setVisible(show_clock)
        self.lbl_day.setVisible(show_clock)
        self.lbl_hijri_date.setVisible(show_clock)
        self.lbl_greg_date.setVisible(show_clock)
'''

text = text.replace('        self.lbl_next_prayer.setVisible(show_countdown)', '        self.lbl_next_prayer.setVisible(show_countdown)' + clock_code)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
