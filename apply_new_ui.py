import re

file_path = r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/widget_ui.py'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# The DesktopWidget class starts at `class DesktopWidget(QWidget):`
# and goes until the end of the file.
desktop_widget_code = '''
class DesktopWidget(QWidget):
    """نافذة الويدجت الشفافة لسطح المكتب"""
    def __init__(self, settings_mgr):
        super().__init__()
        self.settings_mgr = settings_mgr
        self.drag_position = QPoint()

        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow
        if self.settings_mgr.get("stay_on_top", False):
            flags |= Qt.WindowType.WindowStaysOnTopHint
        self.setWindowFlags(flags)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)
        self.setLayoutDirection(Qt.LayoutDirection.RightToLeft)

        self.pray_times_calculator = PrayTimes(self.settings_mgr.get("calc_method", PrayTimes.MAKKAH))

        self.init_ui()
        self.init_tray()
        self.restore_position()

        self.timer = QTimer(self)
        self.timer.timeout.connect(self.update_tick)
        self.timer.start(1000)

        self.update_all()

    def init_ui(self):
        font_path = get_asset_path("tajawal.ttf")
        if os.path.exists(font_path):
            QFontDatabase.addApplicationFont(font_path)

        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)

        self.card = QFrame()
        self.card.setObjectName("widgetCard")
        self.card.setStyleSheet("""
            QFrame#widgetCard {
                background-color: #232323;
                border-radius: 12px;
            }
        """)

        card_layout = QVBoxLayout(self.card)
        card_layout.setContentsMargins(20, 30, 20, 20)
        card_layout.setSpacing(20)

        # 1. Top Section (Day, Temp, Time)
        top_layout = QHBoxLayout()
        top_layout.setContentsMargins(10, 0, 10, 0)
        
        # Right side in RTL (visually left)
        left_layout = QHBoxLayout()
        self.lbl_day = QLabel("الاثنين")
        self.lbl_day.setStyleSheet("font-size: 24px; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
        self.lbl_day.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignLeft)
        
        self.lbl_temp = QLabel("41°")
        self.lbl_temp.setStyleSheet("font-size: 28px; color: #ffffff; font-family: 'Tajawal'; margin-left: 10px; margin-bottom: 10px;")
        self.lbl_temp.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        
        left_layout.addWidget(self.lbl_day)
        left_layout.addWidget(self.lbl_temp)
        left_layout.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignLeft)

        # Left side in RTL (visually right)
        right_layout = QHBoxLayout()
        right_layout.setSpacing(4)
        
        self.lbl_clock = QLabel("6:07")
        self.lbl_clock.setStyleSheet("font-size: 64px; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
        self.lbl_clock.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignRight)
        
        self.lbl_ampm = QLabel("PM")
        self.lbl_ampm.setStyleSheet("font-size: 24px; color: #ffffff; font-family: 'Tajawal'; margin-bottom: 30px;")
        self.lbl_ampm.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        
        right_layout.addWidget(self.lbl_clock)
        right_layout.addWidget(self.lbl_ampm)
        right_layout.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignRight)

        top_layout.addLayout(left_layout)
        top_layout.addStretch()
        top_layout.addLayout(right_layout)

        card_layout.addLayout(top_layout)
        
        card_layout.addSpacing(10)

        # 2. Middle Section (Dates)
        date_layout = QVBoxLayout()
        date_layout.setSpacing(10)
        
        self.lbl_hijri_date = QLabel("1448 / 4 / 3 هـ")
        self.lbl_hijri_date.setStyleSheet("font-size: 32px; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
        self.lbl_hijri_date.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        self.lbl_greg_date = QLabel("2026 / 9 / 14 م")
        self.lbl_greg_date.setStyleSheet("font-size: 32px; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
        self.lbl_greg_date.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        date_layout.addWidget(self.lbl_hijri_date)
        date_layout.addWidget(self.lbl_greg_date)
        card_layout.addLayout(date_layout)
        
        card_layout.addSpacing(30)

        # 3. Prayer Times Row
        self.prayer_cards_layout = QHBoxLayout()
        self.prayer_cards_layout.setSpacing(5)
        
        self.prayer_labels = {}
        # NO Sunrise
        prayers = [
            ("Fajr", "الفجر"), 
            ("Dhuhr", "الظهر"), 
            ("Asr", "العصر"),
            ("Maghrib", "المغرب"), 
            ("Isha", "العشاء")
        ]
        
        for p_key, p_name in prayers:
            p_card = QFrame()
            p_card.setObjectName(f"prayer_{p_key}")
            p_card_layout = QVBoxLayout(p_card)
            p_card_layout.setContentsMargins(10, 15, 10, 15)
            p_card_layout.setSpacing(8)

            name_lbl = QLabel(p_name)
            name_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
            name_lbl.setStyleSheet("font-size: 22px; font-weight: bold; color: #ffffff; border: none; background: transparent; font-family: 'Tajawal';")

            time_lbl = QLabel("--:--")
            time_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
            time_lbl.setStyleSheet("font-size: 22px; font-weight: bold; color: #ffffff; border: none; background: transparent; font-family: 'Tajawal';")
            
            ampm_lbl = QLabel("ص")
            ampm_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
            ampm_lbl.setStyleSheet("font-size: 20px; font-weight: bold; color: #ffffff; border: none; background: transparent; font-family: 'Tajawal';")

            p_card_layout.addWidget(name_lbl)
            p_card_layout.addWidget(time_lbl)
            p_card_layout.addWidget(ampm_lbl)

            p_card.setStyleSheet(f"""
                QFrame#prayer_{p_key} {{
                    background: transparent;
                    border: none;
                    border-radius: 12px;
                }}
            """)

            self.prayer_cards_layout.addWidget(p_card)
            self.prayer_labels[p_key] = {"frame": p_card, "name": name_lbl, "time": time_lbl, "ampm": ampm_lbl}

        card_layout.addLayout(self.prayer_cards_layout)
        
        card_layout.addSpacing(20)

        # 4. Bottom Message
        self.lbl_next_prayer = QLabel("حان الآن وقت صلاة المغرب")
        self.lbl_next_prayer.setStyleSheet("font-size: 24px; font-weight: bold; color: #ef5350; font-family: 'Tajawal';")
        self.lbl_next_prayer.setAlignment(Qt.AlignmentFlag.AlignCenter)
        card_layout.addWidget(self.lbl_next_prayer)
        
        card_layout.addSpacing(10)

        main_layout.addWidget(self.card)
        self.setFixedSize(500, 560)
        
        self.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.customContextMenuRequested.connect(self.show_context_menu)

    def init_tray(self):
        self.tray_icon = QSystemTrayIcon(self)
        icon_path = get_asset_path("app_icon.ico")
        if os.path.exists(icon_path):
            self.tray_icon.setIcon(QIcon(icon_path))
        else:
            self.tray_icon.setIcon(self.windowIcon())

        tray_menu = QMenu()
        tray_menu.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
        tray_menu.setStyleSheet("""
            QMenu { background-color: #1e293b; color: #ffffff; border: 1px solid #475569; border-radius: 6px; padding: 4px; font-family: 'Tajawal', sans-serif; }
            QMenu::item { padding: 6px 20px; border-radius: 4px; }
            QMenu::item:selected { background-color: #0284c7; }
        """)

        act_show = QAction("إظهار / إخفاء الويدجت", self)
        act_show.triggered.connect(self.toggle_visibility)
        tray_menu.addAction(act_show)

        act_calendar = QAction("📅 تقويم الشهر", self)
        act_calendar.triggered.connect(self.show_month_calendar)
        tray_menu.addAction(act_calendar)

        act_settings = QAction("⚙️ الإعدادات", self)
        act_settings.triggered.connect(self.show_settings)
        tray_menu.addAction(act_settings)

        tray_menu.addSeparator()

        act_exit = QAction("خروج", self)
        act_exit.triggered.connect(self.quit_app)
        tray_menu.addAction(act_exit)

        self.tray_icon.setContextMenu(tray_menu)
        self.tray_icon.activated.connect(self.tray_activated)
        self.tray_icon.show()

    def tray_activated(self, reason):
        if reason == QSystemTrayIcon.ActivationReason.Trigger:
            self.toggle_visibility()

    def toggle_visibility(self):
        if self.isVisible():
            self.hide()
        else:
            self.show()
            self.raise_()
            self.activateWindow()

    def show_context_menu(self, pos):
        menu = QMenu(self)
        menu.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
        menu.setStyleSheet("""
            QMenu { background-color: #1a2233; color: #e2e8f0; border: 1px solid rgba(245, 158, 11, 0.3); border-radius: 10px; padding: 6px 4px; font-family: 'Tajawal', 'Segoe UI', sans-serif; font-size: 13px; }
            QMenu::item { padding: 8px 24px; border-radius: 6px; }
            QMenu::item:selected { background-color: rgba(245, 158, 11, 0.2); color: #fbbf24; }
            QMenu::separator { height: 1px; background: rgba(255,255,255,0.08); margin: 4px 8px; }
        """)

        act_calendar = QAction("📅  تقويم الشهر", self)
        act_calendar.triggered.connect(self.show_month_calendar)
        menu.addAction(act_calendar)

        act_settings = QAction("⚙️  الإعدادات", self)
        act_settings.triggered.connect(self.show_settings)
        menu.addAction(act_settings)

        menu.addSeparator()

        act_min = QAction("─  تصغير إلى شريط المهام", self)
        act_min.triggered.connect(self.hide)
        menu.addAction(act_min)

        act_exit = QAction("✕  إغلاق التطبيق", self)
        act_exit.triggered.connect(self.quit_app)
        menu.addAction(act_exit)

        menu.exec(self.mapToGlobal(pos))

    def show_settings(self):
        dlg = SettingsDialog(self.settings_mgr, self)
        if dlg.exec():
            stay_on_top = self.settings_mgr.get("stay_on_top", False)
            flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow
            if stay_on_top:
                flags |= Qt.WindowType.WindowStaysOnTopHint
            self.setWindowFlags(flags)
            self.show()
            self.update_all()

    def show_month_calendar(self):
        dlg = MonthCalendarDialog(self.settings_mgr, self)
        dlg.exec()

    def update_tick(self):
        now = datetime.datetime.now()
        digit_style = self.settings_mgr.get("digit_style", 1)
        time_fmt = self.settings_mgr.get("time_format", 1)

        if time_fmt == 1:
            h = now.hour % 12
            if h == 0: h = 12
            clock_str = f"{h}:{now.minute:02d}"
            ampm_str = "AM" if now.hour < 12 else "PM"
        else:
            clock_str = f"{now.hour:02d}:{now.minute:02d}"
            ampm_str = ""

        self.lbl_clock.setText(localize_digits(clock_str, digit_style))
        self.lbl_ampm.setText(ampm_str)

        if now.second == 0:
            self.update_prayer_times()

    def update_all(self):
        today = datetime.date.today()
        offset_days = self.settings_mgr.get("hijri_offset", 0)
        digit_style = self.settings_mgr.get("digit_style", 1)
        lang = self.settings_mgr.get("language", "ar")

        formatted = format_full_date(today, offset_days, digit_style, lang)
        
        # Override to exact format shown in picture if using arabic
        hy, hm, hd, hname = get_hijri_date(today, offset_days)
        hijri_str = f"{hy} / {hm} / {hd} هـ"
        greg_str = f"{today.year} / {today.month} / {today.day} م"
        
        self.lbl_hijri_date.setText(localize_digits(hijri_str, digit_style))
        self.lbl_greg_date.setText(localize_digits(greg_str, digit_style))
        self.lbl_day.setText(formatted["day_name"])

        self.tray_icon.setToolTip(f"{formatted['day_name']} - {formatted['hijri_formatted']}")
        self.update_tick()
        self.update_prayer_times()

    def update_prayer_times(self):
        today = datetime.date.today()
        lat = self.settings_mgr.get("lat", 24.7136)
        lng = self.settings_mgr.get("lng", 46.6753)
        method_id = self.settings_mgr.get("calc_method", PrayTimes.MAKKAH)
        digit_style = self.settings_mgr.get("digit_style", 1)
        time_fmt = self.settings_mgr.get("time_format", 1)

        self.pray_times_calculator.calc_method = method_id
        self.pray_times_calculator.asr_juristic = self.settings_mgr.get("asr_madhab", PrayTimes.SHAFII)
        times = self.pray_times_calculator.get_times(today, lat, lng)

        now = datetime.datetime.now()
        current_minutes = now.hour * 60 + now.minute

        next_prayer_name = None
        next_prayer_diff = 9999
        active_prayer_key = None
        active_prayer_name = None

        prayers_order = [
            ("Fajr", "الفجر"), ("Dhuhr", "الظهر"), 
            ("Asr", "العصر"), ("Maghrib", "المغرب"), 
            ("Isha", "العشاء")
        ]

        for p_key, p_name in prayers_order:
            t_float = times.get(p_key, 0)
            if time_fmt == 1 and not isinstance(t_float, str):
                h = int(t_float)
                m = int((t_float - h) * 60)
                is_pm = h >= 12
                if h > 12: h -= 12
                if h == 0: h = 12
                time_str = f"{h}:{m:02d}"
                ampm_str = "م" if is_pm else "ص"
            else:
                formatted_time = self.pray_times_calculator.format_time(t_float, time_fmt)
                time_str = formatted_time
                ampm_str = ""

            self.prayer_labels[p_key]["time"].setText(localize_digits(time_str, digit_style))
            self.prayer_labels[p_key]["ampm"].setText(ampm_str)

            if not isinstance(t_float, str) and not (isinstance(t_float, float) and (t_float != t_float)):
                p_mins = int(t_float * 60)
                diff = p_mins - current_minutes
                if diff > 0 and diff < next_prayer_diff:
                    next_prayer_diff = diff
                    next_prayer_name = p_name
                    next_p_key = p_key

                if current_minutes >= p_mins:
                    active_prayer_key = p_key
                    active_prayer_name = p_name

        # If no active prayer today, it means we are before Fajr
        if not active_prayer_key:
            active_prayer_key = "Isha"
            active_prayer_name = "العشاء"

        for p_key, data in self.prayer_labels.items():
            if p_key == active_prayer_key:
                data["frame"].setStyleSheet(f"""
                    QFrame#prayer_{p_key} {{
                        background-color: #d32f2f;
                        border: none;
                        border-radius: 12px;
                    }}
                """)
            else:
                data["frame"].setStyleSheet(f"""
                    QFrame#prayer_{p_key} {{
                        background: transparent;
                        border: none;
                    }}
                """)

        if active_prayer_name:
            self.lbl_next_prayer.setText(f"حان الآن وقت صلاة {active_prayer_name}")
            self.lbl_next_prayer.setStyleSheet("font-size: 24px; font-weight: bold; color: #ef5350; font-family: 'Tajawal';")
        else:
            self.lbl_next_prayer.setText("")

    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.drag_position = event.globalPosition().toPoint() - self.frameGeometry().topLeft()
            event.accept()

    def mouseMoveEvent(self, event):
        if event.buttons() == Qt.MouseButton.LeftButton:
            new_pos = event.globalPosition().toPoint() - self.drag_position
            self.move(new_pos)
            self.settings_mgr.set("pos_x", new_pos.x())
            self.settings_mgr.set("pos_y", new_pos.y())
            event.accept()

    def restore_position(self):
        px = self.settings_mgr.get("pos_x", -1)
        py = self.settings_mgr.get("pos_y", -1)
        if px >= 0 and py >= 0:
            self.move(px, py)
        else:
            screen = self.screen().geometry()
            self.move(screen.width() - self.width() - 40, 60)

    def quit_app(self):
        self.settings_mgr.set("pos_x", self.x())
        self.settings_mgr.set("pos_y", self.y())
        self.settings_mgr.save()
        self.tray_icon.hide()
        sys.exit(0)
'''

import re
match = re.search(r'class DesktopWidget\(QWidget\):.*', content, re.DOTALL)
if match:
    new_content = content[:match.start()] + desktop_widget_code
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Success")
else:
    print("DesktopWidget class not found")
