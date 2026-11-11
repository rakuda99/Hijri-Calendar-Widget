import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Apply asr_madhab
asr_logic = '''
        self.pray_times_calculator.calc_method = method_id
        self.pray_times_calculator.asr_juristic = self.settings_mgr.get("asr_madhab", PrayTimes.SHAFII)
        times = self.pray_times_calculator.get_times(today, lat, lng)
'''
text = text.replace('        self.pray_times_calculator.calc_method = method_id\n        times = self.pray_times_calculator.get_times(today, lat, lng)', asr_logic.strip('\n'))

# 2. Apply Visibility settings
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

text = re.sub(
    r'        # Countdown string.*?self\.lbl_next_prayer\.setText\(next_prayer_str\)',
    r'\g<0>' + '\n' + visibility_code,
    text,
    flags=re.DOTALL
)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
