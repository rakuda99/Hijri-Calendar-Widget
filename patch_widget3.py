import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# Update prayer name styles in update_prayer_times
new_update_prayer = '''
        for p_key, data in self.prayer_labels.items():
            fs = self.settings_mgr.get("prayer_font_size", 13)
            col = self.settings_mgr.get("prayer_color", "#f8fafc")
            data["time"].setStyleSheet(f"font-size: {fs}px; font-weight: bold; color: {col}; border: none; background: transparent;")

            pn_fs = self.settings_mgr.get("prayer_name_font_size", 12)
            pn_col = self.settings_mgr.get("prayer_name_color", "#94a3b8")

            if p_key == active_prayer_key:
                # Removed active prayer border completely (border: none)
                data["frame"].setStyleSheet(f"""
                    QFrame#prayer_{p_key} {{
                        background: qlineargradient(x1:0, y1:0, x2:0, y2:1, stop:0 rgba(14, 165, 233, 0.35), stop:1 rgba(2, 132, 199, 0.45));
                        border: none;
                        border-radius: 10px;
                    }}
                """)
                data["name"].setStyleSheet(f"font-size: {pn_fs}px; font-weight: bold; color: #38bdf8; border: none; background: transparent;")
            else:
                _alpha = self.settings_mgr.get("card_opacity", 70) / 100.0
                data["frame"].setStyleSheet(f"""
                    QFrame#prayer_{p_key} {{
                        background-color: rgba(30, 41, 59, {_alpha});
                        border: none;
                        border-radius: 10px;
                    }}
                """)
                data["name"].setStyleSheet(f"font-size: {pn_fs}px; font-weight: bold; color: {pn_col}; border: none; background: transparent;")
'''

text = re.sub(
    r'        for p_key, data in self\.prayer_labels\.items\(\):.*?data\["name"\]\.setStyleSheet\(.*?transparent;"\)',
    new_update_prayer.strip('\n'),
    text,
    flags=re.DOTALL
)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
