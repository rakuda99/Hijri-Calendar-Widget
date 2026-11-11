import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Add fields to SettingsDialog
settings_code = '''        layout.addWidget(self.combo_time_format)

        # --- NEW SETTINGS ---
        layout.addWidget(QLabel("حجم خط أوقات الصلاة:"))
        self.spin_font_size = QSpinBox()
        self.spin_font_size.setRange(10, 30)
        self.spin_font_size.setValue(self.settings_mgr.get("prayer_font_size", 13))
        layout.addWidget(self.spin_font_size)

        layout.addWidget(QLabel("لون أوقات الصلاة:"))
        self.combo_color = QComboBox()
        self.combo_color.addItem("أبيض (افتراضي)", "#f8fafc")
        self.combo_color.addItem("أزرق سماوي", "#38bdf8")
        self.combo_color.addItem("ذهبي", "#fbbf24")
        self.combo_color.addItem("أخضر", "#4ade80")
        curr_col = self.settings_mgr.get("prayer_color", "#f8fafc")
        for i in range(self.combo_color.count()):
            if self.combo_color.itemData(i) == curr_col:
                self.combo_color.setCurrentIndex(i)
                break
        layout.addWidget(self.combo_color)
        # -------------------'''
text = re.sub(r'layout\.addWidget\(self\.combo_time_format\)', settings_code, text)

# 2. Add to save_settings
save_code = '''        self.settings_mgr.set("time_format", self.combo_time_format.currentData())
        self.settings_mgr.set("prayer_font_size", self.spin_font_size.value())
        self.settings_mgr.set("prayer_color", self.combo_color.currentData())'''
text = re.sub(r'self\.settings_mgr\.set\(\"time_format\".*?\)', save_code, text)

# 3. Add to update_prayer_times so it refreshes live
update_code = '''        for p_key, data in self.prayer_labels.items():
            fs = self.settings_mgr.get("prayer_font_size", 13)
            col = self.settings_mgr.get("prayer_color", "#f8fafc")
            data["time"].setStyleSheet(f"font-size: {fs}px; font-weight: bold; color: {col};")

            if p_key == active_prayer_key:'''
text = re.sub(r'for p_key, data in self\.prayer_labels\.items\(\):\s*if p_key == active_prayer_key:', update_code, text)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
