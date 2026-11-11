import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Fix missing addLayout for d_lay and g_lay
text = text.replace('        d_lay.addWidget(self.spin_day_sz); d_lay.addWidget(self.combo_day_col)', '        d_lay.addWidget(self.spin_day_sz); d_lay.addWidget(self.combo_day_col)\n        layout.addLayout(d_lay)')
text = text.replace('        g_lay.addWidget(self.spin_greg_sz); g_lay.addWidget(self.combo_greg_col)', '        g_lay.addWidget(self.spin_greg_sz); g_lay.addWidget(self.combo_greg_col)\n        layout.addLayout(g_lay)')

# 2. Add the visibility logic at the end of update_prayer_times
visibility_code = '''
        # Apply Visibility Settings
        show_sunrise = self.settings_mgr.get("show_sunrise", True)
        show_prayers = self.settings_mgr.get("show_prayers", True)
        next_only = self.settings_mgr.get("next_prayer_only", False)
        show_countdown = self.settings_mgr.get("show_countdown", True)
        
        self.lbl_next_prayer.setVisible(show_countdown)

        for p_key, data in self.prayer_labels.items():
            if not show_prayers:
                data["frame"].hide()
                continue
                
            if p_key == "Sunrise" and not show_sunrise:
                data["frame"].hide()
                continue
                
            if next_only and p_key != active_prayer_key:
                data["frame"].hide()
                continue
                
            data["frame"].show()
'''

text = text.replace('            self.lbl_next_prayer.setText(f"تقبل الله طاعتكم ({self.settings_mgr.get(\'city\', \'\').split(\' (\')[0]})")', '            self.lbl_next_prayer.setText(f"تقبل الله طاعتكم ({self.settings_mgr.get(\'city\', \'\').split(\' (\')[0]})")' + '\n' + visibility_code)

# 3. Fix update_all to call update_prayer_times
text = text.replace('        self.lbl_day.setText(formatted["day_name"])', '        self.update_prayer_times()\n        self.lbl_day.setText(formatted["day_name"])')

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
