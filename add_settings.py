import re
with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Add Asr Madhab to SettingsDialog
asr_ui = '''
        layout.addWidget(QLabel("مذهب العصر (Asr Madhab):"))
        self.combo_asr = QComboBox()
        self.combo_asr.addItem("شافعي، مالكي، حنبلي (Standard)", PrayTimes.SHAFII)
        self.combo_asr.addItem("حنفي (Hanafi)", PrayTimes.HANAFI)
        curr_asr = self.settings_mgr.get("asr_madhab", PrayTimes.SHAFII)
        self.combo_asr.setCurrentIndex(1 if curr_asr == PrayTimes.HANAFI else 0)
        layout.addWidget(self.combo_asr)
'''
text = text.replace('layout.addWidget(self.combo_method)', 'layout.addWidget(self.combo_method)' + asr_ui)
text = text.replace('self.settings_mgr.set("calc_method", self.combo_method.currentData())', 'self.settings_mgr.set("calc_method", self.combo_method.currentData())\n        self.settings_mgr.set("asr_madhab", self.combo_asr.currentData())')

# 2. Add Display Checkboxes to SettingsDialog
checks_ui = '''
        layout.addWidget(QLabel("إعدادات العرض (Display Settings):"))
        
        self.chk_sunrise = QCheckBox("عرض وقت الشروق (Show Sunrise)")
        self.chk_sunrise.setChecked(self.settings_mgr.get("show_sunrise", True))
        layout.addWidget(self.chk_sunrise)

        self.chk_clock = QCheckBox("عرض الساعة الحالية (Show Clock)")
        self.chk_clock.setChecked(self.settings_mgr.get("show_clock", True))
        layout.addWidget(self.chk_clock)

        self.chk_countdown = QCheckBox("عرض الوقت المتبقي (Show Remaining Time)")
        self.chk_countdown.setChecked(self.settings_mgr.get("show_countdown", True))
        layout.addWidget(self.chk_countdown)

        self.chk_prayers = QCheckBox("عرض بطاقات الصلوات (Show Prayer Times)")
        self.chk_prayers.setChecked(self.settings_mgr.get("show_prayers", True))
        layout.addWidget(self.chk_prayers)

        self.chk_next_only = QCheckBox("الصلاة القادمة فقط (Next Prayer Only)")
        self.chk_next_only.setChecked(self.settings_mgr.get("next_prayer_only", False))
        layout.addWidget(self.chk_next_only)
'''
text = text.replace('layout.addWidget(self.chk_ontop)', 'layout.addWidget(self.chk_ontop)' + checks_ui)

save_checks = '''
        self.settings_mgr.set("show_sunrise", self.chk_sunrise.isChecked())
        self.settings_mgr.set("show_clock", self.chk_clock.isChecked())
        self.settings_mgr.set("show_countdown", self.chk_countdown.isChecked())
        self.settings_mgr.set("show_prayers", self.chk_prayers.isChecked())
        self.settings_mgr.set("next_prayer_only", self.chk_next_only.isChecked())
'''
text = text.replace('self.settings_mgr.set("stay_on_top", self.chk_ontop.isChecked())', 'self.settings_mgr.set("stay_on_top", self.chk_ontop.isChecked())' + save_checks)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
