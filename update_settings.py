import re

with open(r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

new_settings_dialog = '''
class SettingsDialog(QDialog):
    def __init__(self, settings_mgr, parent=None):
        super().__init__(parent)
        self.settings_mgr = settings_mgr
        self.setWindowTitle("Clock Settings / إعدادات الساعة والويدجت")
        self.setWindowFlags(self.windowFlags() & ~Qt.WindowType.WindowContextHelpButtonHint)
        self.resize(500, 750)
        self.setMinimumWidth(400)
        self.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
        self.init_ui()

    def init_ui(self):
        self.setStyleSheet("""
            QDialog { background-color: #121212; color: #ffffff; font-family: 'Tajawal', sans-serif; }
            QLabel { color: #f0f0f0; font-size: 14px; font-weight: bold; }
            QComboBox { background-color: #1e1e1e; color: #ffffff; border: 1px solid #333; border-radius: 6px; padding: 5px; min-height: 30px; font-size: 14px; }
            QComboBox::drop-down { border: none; }
            QPushButton { background-color: #4CAF50; color: white; border: none; border-radius: 8px; padding: 12px; font-size: 16px; font-weight: bold; }
            QPushButton:hover { background-color: #45a049; }
            QCheckBox { color: #f0f0f0; font-size: 14px; font-weight: bold; }
            QCheckBox::indicator { width: 40px; height: 20px; border-radius: 10px; background-color: #555; }
            QCheckBox::indicator:checked { background-color: #00d2b4; }
            QScrollArea { border: none; background: transparent; }
            QWidget#scrollContainer { background: transparent; }
        """)

        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)

        header = QLabel("إعدادات الساعة والويدجت")
        header.setStyleSheet("font-size: 24px; font-weight: bold; color: #ffffff; padding: 20px; background-color: #5000ff;")
        header.setAlignment(Qt.AlignmentFlag.AlignCenter)
        main_layout.addWidget(header)

        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        container = QWidget()
        container.setObjectName("scrollContainer")
        layout = QVBoxLayout(container)
        layout.setContentsMargins(20, 20, 20, 20)
        layout.setSpacing(15)

        def add_combo(label_text, options, key, default):
            layout.addWidget(QLabel(label_text))
            combo = QComboBox()
            for text, val in options:
                combo.addItem(text, val)
            curr = self.settings_mgr.get(key, default)
            for i in range(combo.count()):
                if combo.itemData(i) == curr:
                    combo.setCurrentIndex(i)
                    break
            layout.addWidget(combo)
            return combo
            
        def add_split_combos(l1, opt1, k1, def1, l2, opt2, k2, def2):
            row = QHBoxLayout()
            c1_lay = QVBoxLayout()
            c1_lay.addWidget(QLabel(l1))
            combo1 = QComboBox()
            for text, val in opt1: combo1.addItem(text, val)
            curr1 = self.settings_mgr.get(k1, def1)
            for i in range(combo1.count()):
                if combo1.itemData(i) == curr1: combo1.setCurrentIndex(i); break
            c1_lay.addWidget(combo1)
            row.addLayout(c1_lay)
            
            c2_lay = QVBoxLayout()
            c2_lay.addWidget(QLabel(l2))
            combo2 = QComboBox()
            for text, val in opt2: combo2.addItem(text, val)
            curr2 = self.settings_mgr.get(k2, def2)
            for i in range(combo2.count()):
                if combo2.itemData(i) == curr2: combo2.setCurrentIndex(i); break
            c2_lay.addWidget(combo2)
            row.addLayout(c2_lay)
            layout.addLayout(row)
            return combo1, combo2

        def add_toggle(label_text, key, default):
            chk = QCheckBox(label_text)
            chk.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
            chk.setChecked(self.settings_mgr.get(key, default))
            layout.addWidget(chk)
            return chk

        sizes = [("صغير", 1), ("متوسط", 2), ("كبير", 3)]
        colors = [("أبيض", "#ffffff"), ("أسود", "#000000"), ("ذهبي", "#fbbf24"), ("أزرق", "#38bdf8"), ("أخضر", "#4ade80"), ("أحمر", "#f44336")]

        self.cb_lang = add_combo("Language / اللغة", [("العربية", "ar"), ("English", "en")], "language", "ar")
        self.cb_digit = add_combo("Digit Style / شكل الأرقام", [("أرقام عربية (0-9)", 0), ("أرقام مشرقية (٠-٩)", 1)], "digit_style", 0)
        
        self.cb_c_color, self.cb_c_size = add_split_combos("Clock Color / لون الساعة", colors, "clock_color", "#ffffff", "Clock Size / الحجم", sizes, "clock_size", 2)
        self.chk_show_clk = add_toggle("Show Clock / عرض الساعة", "show_clock", True)
        self.chk_large_lbl = add_toggle("Large Time Labels / تكبير مسميات الوقت", "large_time_labels", False)
        
        self.cb_h_color, self.cb_h_size = add_split_combos("Hijri Color / لون الهجري", colors, "hijri_color", "#ffffff", "Hijri Size / الحجم", sizes, "hijri_size", 2)
        self.chk_h_sym = add_toggle("Show Hijri Symbol (هـ)", "show_hijri_symbol", True)
        self.chk_show_h = add_toggle("Show Hijri Date / عرض التاريخ الهجري", "show_hijri", True)
        self.cb_h_adj = add_combo("Hijri Adjustment / تعديل الهجري", [(f"{i} {'(افتراضي)' if i==0 else ''}", i) for i in range(-3,4)], "hijri_offset", 0)
        
        btn_sync = QPushButton("مزامنة الشبكة")
        btn_sync.setStyleSheet("background-color: #f44336; color: white;")
        layout.addWidget(btn_sync)

        self.cb_g_color, self.cb_g_size = add_split_combos("Gregorian Color / لون الميلادي", colors, "greg_color", "#ffffff", "Gregorian Size / الحجم", sizes, "greg_size", 2)
        self.chk_g_sym = add_toggle("Show Greg Symbol (م)", "show_greg_symbol", True)
        self.chk_show_g = add_toggle("Show Gregorian / عرض الميلادي", "show_greg", True)
        
        self.chk_ar_ampm = add_toggle("Arabic AM/PM / ص، م", "arabic_ampm", True)
        self.chk_24h = add_toggle("Use 24-Hour / نظام 24 ساعة", "time_format", False)
        
        self.cb_bg = add_combo("Background / الخلفية", [("شفاف", "transparent"), ("بطاقة معتمة", "solid")], "background_style", "transparent")
        self.cb_mo_fmt = add_combo("Month Format / تنسيق الشهر", [("اليوم ورقم الشهر", "num"), ("اسم الشهر", "name")], "month_format", "num")
        self.cb_d_color = add_combo("Day Color / لون اليوم", colors, "day_color", "#ffffff")
        self.cb_d_disp = add_combo("Day Display / عرض اليوم", [("Arabic Only / عربي فقط", "ar"), ("English Only", "en")], "day_display", "ar")
        
        self.chk_d_left = add_toggle("Day Text on Left / الأيام يسار الساعة", "day_on_left", True)
        self.chk_d_horiz = add_toggle("Show Days Horizontally / عرض الأيام بشكل أفقي", "days_horizontal", True)
        self.chk_temp = add_toggle("Show Temperature / عرض درجة الحرارة", "show_temp", True)
        
        self.cb_upd = add_combo("Update Interval / وقت التحديث", [("15 دقيقة", 15), ("30 دقيقة", 30), ("60 دقيقة", 60)], "update_interval", 30)

        btn_save = QPushButton("حفظ الإعدادات")
        btn_save.clicked.connect(self.save_and_close)
        layout.addWidget(btn_save)

        scroll.setWidget(container)
        main_layout.addWidget(scroll)

    def save_and_close(self):
        self.settings_mgr.set("language", self.cb_lang.currentData())
        self.settings_mgr.set("digit_style", self.cb_digit.currentData())
        self.settings_mgr.set("clock_color", self.cb_c_color.currentData())
        self.settings_mgr.set("clock_size", self.cb_c_size.currentData())
        self.settings_mgr.set("show_clock", self.chk_show_clk.isChecked())
        self.settings_mgr.set("large_time_labels", self.chk_large_lbl.isChecked())
        self.settings_mgr.set("hijri_color", self.cb_h_color.currentData())
        self.settings_mgr.set("hijri_size", self.cb_h_size.currentData())
        self.settings_mgr.set("show_hijri_symbol", self.chk_h_sym.isChecked())
        self.settings_mgr.set("show_hijri", self.chk_show_h.isChecked())
        self.settings_mgr.set("hijri_offset", self.cb_h_adj.currentData())
        self.settings_mgr.set("greg_color", self.cb_g_color.currentData())
        self.settings_mgr.set("greg_size", self.cb_g_size.currentData())
        self.settings_mgr.set("show_greg_symbol", self.chk_g_sym.isChecked())
        self.settings_mgr.set("show_greg", self.chk_show_g.isChecked())
        self.settings_mgr.set("arabic_ampm", self.chk_ar_ampm.isChecked())
        self.settings_mgr.set("time_format", self.chk_24h.isChecked())
        self.settings_mgr.set("background_style", self.cb_bg.currentData())
        self.settings_mgr.set("month_format", self.cb_mo_fmt.currentData())
        self.settings_mgr.set("day_color", self.cb_d_color.currentData())
        self.settings_mgr.set("day_display", self.cb_d_disp.currentData())
        self.settings_mgr.set("day_on_left", self.chk_d_left.isChecked())
        self.settings_mgr.set("days_horizontal", self.chk_d_horiz.isChecked())
        self.settings_mgr.set("show_temp", self.chk_temp.isChecked())
        self.settings_mgr.set("update_interval", self.cb_upd.currentData())
        
        self.accept()
'''

m1 = re.search(r'class SettingsDialog\(QDialog\):.*?class MonthCalendarDialog', text, re.DOTALL)
if m1:
    text = text[:m1.start()] + new_settings_dialog + "\n\nclass MonthCalendarDialog" + text[m1.end():]
else:
    print("Could not find SettingsDialog")

with open(r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
