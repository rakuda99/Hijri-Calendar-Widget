import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Add English to prayers_order in DesktopWidget
text = text.replace(
'''        prayers = [
            ("Fajr", "الفجر"),
            ("Sunrise", "الشروق"),
            ("Dhuhr", "الظهر"),
            ("Asr", "العصر"),
            ("Maghrib", "المغرب"),
            ("Isha", "العشاء")
        ]''',
'''        lang = self.settings_mgr.get("language", "ar")
        if lang == "en":
            prayers = [
                ("Fajr", "Fajr"), ("Sunrise", "Sunrise"),
                ("Dhuhr", "Dhuhr"), ("Asr", "Asr"),
                ("Maghrib", "Maghrib"), ("Isha", "Isha")
            ]
        else:
            prayers = [
                ("Fajr", "الفجر"), ("Sunrise", "الشروق"),
                ("Dhuhr", "الظهر"), ("Asr", "العصر"),
                ("Maghrib", "المغرب"), ("Isha", "العشاء")
            ]'''
)

text = text.replace(
'''        prayers_order = [
            ("Fajr", "الفجر"),
            ("Sunrise", "الشروق"),
            ("Dhuhr", "الظهر"),
            ("Asr", "العصر"),
            ("Maghrib", "المغرب"),
            ("Isha", "العشاء")
        ]''',
'''        lang = self.settings_mgr.get("language", "ar")
        if lang == "en":
            prayers_order = [
                ("Fajr", "Fajr"), ("Sunrise", "Sunrise"),
                ("Dhuhr", "Dhuhr"), ("Asr", "Asr"),
                ("Maghrib", "Maghrib"), ("Isha", "Isha")
            ]
        else:
            prayers_order = [
                ("Fajr", "الفجر"), ("Sunrise", "الشروق"),
                ("Dhuhr", "الظهر"), ("Asr", "العصر"),
                ("Maghrib", "المغرب"), ("Isha", "العشاء")
            ]'''
)

# 2. Update format_full_date call in update_all
text = text.replace(
    'formatted = format_full_date(today, offset_days, digit_style)',
    'lang = self.settings_mgr.get("language", "ar")\n        formatted = format_full_date(today, offset_days, digit_style, lang)'
)

# 3. Add Language to Settings Dialog
lang_ui = '''
        # Language
        layout.addWidget(QLabel("لغة الويدجت (Language):"))
        self.combo_lang = QComboBox()
        self.combo_lang.addItem("العربية", "ar")
        self.combo_lang.addItem("English", "en")
        curr_lang = self.settings_mgr.get("language", "ar")
        self.combo_lang.setCurrentIndex(0 if curr_lang == "ar" else 1)
        layout.addWidget(self.combo_lang)
'''
text = text.replace('layout.addWidget(self.combo_time_format)', 'layout.addWidget(self.combo_time_format)' + lang_ui)
text = text.replace('self.settings_mgr.set("time_format", self.combo_time_format.currentData())', 'self.settings_mgr.set("time_format", self.combo_time_format.currentData())\n        self.settings_mgr.set("language", self.combo_lang.currentData())')

# 4. Remove lbl_day background
text = re.sub(
    r'self\.lbl_day\.setStyleSheet\("""\s*background-color: rgba\(56, 189, 248, 0\.15\);.*?border-radius: 6px;\s*"""\)',
    'self.lbl_day.setStyleSheet("font-size: 14px; font-weight: bold; color: #38bdf8; background: transparent; border: none;")',
    text, flags=re.DOTALL
)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
