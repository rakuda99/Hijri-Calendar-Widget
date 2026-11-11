import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

update_all_style = '''
        # Update styles
        df_sz = self.settings_mgr.get("day_font_size", 14)
        df_col = self.settings_mgr.get("day_color", "#38bdf8")
        self.lbl_day.setStyleSheet(f"font-size: {df_sz}px; font-weight: bold; color: {df_col}; background: transparent; border: none;")

        gf_sz = self.settings_mgr.get("greg_font_size", 13)
        gf_col = self.settings_mgr.get("greg_color", "#94a3b8")
        self.lbl_greg_date.setStyleSheet(f"font-size: {gf_sz}px; color: {gf_col}; font-family: 'Tajawal';")

        am_sz = self.settings_mgr.get("ampm_font_size", 14)
        am_col = self.settings_mgr.get("ampm_color", "#38bdf8")
        self.lbl_ampm.setStyleSheet(f"font-size: {am_sz}px; font-weight: bold; color: {am_col}; margin-top: -6px;")
'''

text = text.replace('self.lbl_day.setText(formatted["day_name"])', update_all_style + '\n        self.lbl_day.setText(formatted["day_name"])')

# Add these settings to SettingsDialog
new_settings = '''
        # Advanced Colors & Fonts
        layout.addWidget(QLabel("حجم ولون اسم اليوم:"))
        d_lay = QHBoxLayout()
        self.spin_day_sz = QSpinBox(); self.spin_day_sz.setRange(10, 30); self.spin_day_sz.setValue(self.settings_mgr.get("day_font_size", 14))
        self.combo_day_col = QComboBox(); self.combo_day_col.addItems(["#38bdf8", "#fbbf24", "#4ade80", "#f8fafc", "#94a3b8"])
        self.combo_day_col.setCurrentText(self.settings_mgr.get("day_color", "#38bdf8"))
        d_lay.addWidget(self.spin_day_sz); d_lay.addWidget(self.combo_day_col)
        layout.addLayout(d_lay)

        layout.addWidget(QLabel("حجم ولون التاريخ الميلادي:"))
        g_lay = QHBoxLayout()
        self.spin_greg_sz = QSpinBox(); self.spin_greg_sz.setRange(10, 30); self.spin_greg_sz.setValue(self.settings_mgr.get("greg_font_size", 13))
        self.combo_greg_col = QComboBox(); self.combo_greg_col.addItems(["#94a3b8", "#f8fafc", "#fbbf24", "#38bdf8", "#4ade80"])
        self.combo_greg_col.setCurrentText(self.settings_mgr.get("greg_color", "#94a3b8"))
        g_lay.addWidget(self.spin_greg_sz); g_lay.addWidget(self.combo_greg_col)
        layout.addLayout(g_lay)

        layout.addWidget(QLabel("حجم ولون اسم الصلاة (الفجر، إلخ):"))
        pn_lay = QHBoxLayout()
        self.spin_pn_sz = QSpinBox(); self.spin_pn_sz.setRange(10, 30); self.spin_pn_sz.setValue(self.settings_mgr.get("prayer_name_font_size", 12))
        self.combo_pn_col = QComboBox(); self.combo_pn_col.addItems(["#94a3b8", "#f8fafc", "#fbbf24", "#38bdf8", "#4ade80"])
        self.combo_pn_col.setCurrentText(self.settings_mgr.get("prayer_name_color", "#94a3b8"))
        pn_lay.addWidget(self.spin_pn_sz); pn_lay.addWidget(self.combo_pn_col)
        layout.addLayout(pn_lay)
'''
text = text.replace('layout.addWidget(self.combo_color)', 'layout.addWidget(self.combo_color)' + new_settings)

save_advanced = '''
        self.settings_mgr.set("day_font_size", self.spin_day_sz.value())
        self.settings_mgr.set("day_color", self.combo_day_col.currentText())
        self.settings_mgr.set("greg_font_size", self.spin_greg_sz.value())
        self.settings_mgr.set("greg_color", self.combo_greg_col.currentText())
        self.settings_mgr.set("prayer_name_font_size", self.spin_pn_sz.value())
        self.settings_mgr.set("prayer_name_color", self.combo_pn_col.currentText())
'''
text = text.replace('self.settings_mgr.set("prayer_color", self.combo_color.currentData())', 'self.settings_mgr.set("prayer_color", self.combo_color.currentData())' + save_advanced)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
