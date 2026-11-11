import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

colors_list_code = '''
        def populate_colors(combo, current_val):
            combo.clear()
            colors = [("أزرق", "#38bdf8"), ("ذهبي", "#fbbf24"), ("أخضر", "#4ade80"), ("أبيض", "#f8fafc"), ("رمادي", "#94a3b8")]
            for name, val in colors:
                combo.addItem(name, val)
            for i in range(combo.count()):
                if combo.itemData(i) == current_val:
                    combo.setCurrentIndex(i)
                    break

        self.spin_day_sz = QSpinBox(); self.spin_day_sz.setRange(10, 30); self.spin_day_sz.setValue(self.settings_mgr.get("day_font_size", 14))
        self.combo_day_col = QComboBox(); populate_colors(self.combo_day_col, self.settings_mgr.get("day_color", "#38bdf8"))
        d_lay.addWidget(self.spin_day_sz); d_lay.addWidget(self.combo_day_col)

        layout.addWidget(QLabel("حجم ولون التاريخ الميلادي:"))
        g_lay = QHBoxLayout()
        self.spin_greg_sz = QSpinBox(); self.spin_greg_sz.setRange(10, 30); self.spin_greg_sz.setValue(self.settings_mgr.get("greg_font_size", 13))
        self.combo_greg_col = QComboBox(); populate_colors(self.combo_greg_col, self.settings_mgr.get("greg_color", "#94a3b8"))
        g_lay.addWidget(self.spin_greg_sz); g_lay.addWidget(self.combo_greg_col)

        layout.addWidget(QLabel("حجم ولون اسم الصلاة (الفجر، إلخ):"))
        pn_lay = QHBoxLayout()
        self.spin_pn_sz = QSpinBox(); self.spin_pn_sz.setRange(10, 30); self.spin_pn_sz.setValue(self.settings_mgr.get("prayer_name_font_size", 12))
        self.combo_pn_col = QComboBox(); populate_colors(self.combo_pn_col, self.settings_mgr.get("prayer_name_color", "#94a3b8"))
        pn_lay.addWidget(self.spin_pn_sz); pn_lay.addWidget(self.combo_pn_col)

        layout.addWidget(QLabel("نمط خلفية أوقات الصلاة:"))
        self.combo_bg_style = QComboBox()
        self.combo_bg_style.addItem("بخلفية شفافة جزئياً", "card")
        self.combo_bg_style.addItem("شفاف بالكامل (بدون خلفية)", "transparent")
        bg_style = self.settings_mgr.get("bg_style", "card")
        self.combo_bg_style.setCurrentIndex(0 if bg_style == "card" else 1)
        layout.addWidget(self.combo_bg_style)
'''

text = re.sub(
    r'self\.spin_day_sz = QSpinBox\(\).*?pn_lay\.addWidget\(self\.combo_pn_col\)',
    colors_list_code.strip('\n'),
    text,
    flags=re.DOTALL
)

# And for save settings:
save_code = '''
        self.settings_mgr.set("day_font_size", self.spin_day_sz.value())
        self.settings_mgr.set("day_color", self.combo_day_col.currentData())
        self.settings_mgr.set("greg_font_size", self.spin_greg_sz.value())
        self.settings_mgr.set("greg_color", self.combo_greg_col.currentData())
        self.settings_mgr.set("prayer_name_font_size", self.spin_pn_sz.value())
        self.settings_mgr.set("prayer_name_color", self.combo_pn_col.currentData())
        self.settings_mgr.set("bg_style", self.combo_bg_style.currentData())
'''
text = re.sub(
    r'self\.settings_mgr\.set\("day_font_size".*?self\.combo_pn_col\.currentText\(\)\)',
    save_code.strip('\n'),
    text,
    flags=re.DOTALL
)


with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
