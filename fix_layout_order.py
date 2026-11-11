import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# I will replace the messy section with a clean layout.
messy_start = '        # Advanced Colors & Fonts'
messy_end = '        # -------------------'

replacement = '''        # Advanced Colors & Fonts
        d_lay = QHBoxLayout()
        d_lay.addWidget(QLabel("حجم ولون اسم اليوم:"))
        self.spin_day_sz = QSpinBox(); self.spin_day_sz.setRange(10, 30); self.spin_day_sz.setValue(self.settings_mgr.get("day_font_size", 14))
        self.combo_day_col = QComboBox(); populate_colors(self.combo_day_col, self.settings_mgr.get("day_color", "#38bdf8"))
        d_lay.addWidget(self.spin_day_sz)
        d_lay.addWidget(self.combo_day_col)
        layout.addLayout(d_lay)

        g_lay = QHBoxLayout()
        g_lay.addWidget(QLabel("حجم ولون التاريخ الميلادي:"))
        self.spin_greg_sz = QSpinBox(); self.spin_greg_sz.setRange(10, 30); self.spin_greg_sz.setValue(self.settings_mgr.get("greg_font_size", 13))
        self.combo_greg_col = QComboBox(); populate_colors(self.combo_greg_col, self.settings_mgr.get("greg_color", "#94a3b8"))
        g_lay.addWidget(self.spin_greg_sz)
        g_lay.addWidget(self.combo_greg_col)
        layout.addLayout(g_lay)

        pn_lay = QHBoxLayout()
        pn_lay.addWidget(QLabel("حجم ولون اسم الصلاة (الفجر، إلخ):"))
        self.spin_pn_sz = QSpinBox(); self.spin_pn_sz.setRange(10, 30); self.spin_pn_sz.setValue(self.settings_mgr.get("prayer_name_font_size", 12))
        self.combo_pn_col = QComboBox(); populate_colors(self.combo_pn_col, self.settings_mgr.get("prayer_name_color", "#94a3b8"))
        pn_lay.addWidget(self.spin_pn_sz)
        pn_lay.addWidget(self.combo_pn_col)
        layout.addLayout(pn_lay)

        bg_lay = QHBoxLayout()
        bg_lay.addWidget(QLabel("نمط خلفية أوقات الصلاة:"))
        self.combo_bg_style = QComboBox()
        self.combo_bg_style.addItem("بطاقات نصف شفافة (افتراضي)", "card")
        self.combo_bg_style.addItem("شفاف بالكامل (بدون خلفية)", "transparent")
        bg_style = self.settings_mgr.get("bg_style", "card")
        self.combo_bg_style.setCurrentIndex(0 if bg_style == "card" else 1)
        bg_lay.addWidget(self.combo_bg_style)
        layout.addLayout(bg_lay)

        # -------------------'''

start_idx = text.find(messy_start)
end_idx = text.find(messy_end) + len(messy_end)
text = text[:start_idx] + replacement + text[end_idx:]

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
