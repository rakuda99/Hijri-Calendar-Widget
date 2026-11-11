import re

with open(r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/main.py', 'r', encoding='utf-8') as f:
    main_text = f.read()

new_main = main_text.replace(
    'from desktop_app.widget_ui import DesktopWidget, get_asset_path',
    'from desktop_app.widget_ui import AppManager, get_asset_path'
).replace(
    'widget = DesktopWidget(settings_mgr)',
    'manager = AppManager(settings_mgr)'
).replace(
    'widget.show()',
    'manager.show_all()'
)

with open(r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/main.py', 'w', encoding='utf-8') as f:
    f.write(new_main)


with open(r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    ui_text = f.read()

# We need to replace DesktopWidget with ClockWidget, PrayerWidget, and AppManager
# The DesktopWidget class starts at `class DesktopWidget(QWidget):`
m2 = re.search(r'class DesktopWidget\(QWidget\):.*', ui_text, re.DOTALL)
if m2:
    base_text = ui_text[:m2.start()]
else:
    base_text = ui_text


new_classes = '''
class ClockWidget(QWidget):
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

        self.init_ui()
        self.restore_position()

    def init_ui(self):
        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)

        self.card = QFrame()
        self.card.setObjectName("clockCard")
        self.card.setStyleSheet("QFrame#clockCard { background-color: #232323; border-radius: 12px; }")

        card_layout = QVBoxLayout(self.card)
        card_layout.setContentsMargins(20, 30, 20, 20)
        card_layout.setSpacing(20)

        self.top_layout = QHBoxLayout()
        self.top_layout.setContentsMargins(10, 0, 10, 0)
        
        self.left_layout = QHBoxLayout()
        self.lbl_day = QLabel("الاثنين")
        self.lbl_day.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignLeft)
        self.lbl_temp = QLabel("41°")
        self.lbl_temp.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        
        self.left_layout.addWidget(self.lbl_day)
        self.left_layout.addWidget(self.lbl_temp)
        self.left_layout.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignLeft)

        self.right_layout = QHBoxLayout()
        self.right_layout.setSpacing(4)
        
        self.lbl_clock = QLabel("6:07")
        self.lbl_clock.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignRight)
        self.lbl_ampm = QLabel("PM")
        self.lbl_ampm.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        
        self.right_layout.addWidget(self.lbl_clock)
        self.right_layout.addWidget(self.lbl_ampm)
        self.right_layout.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignRight)

        self.top_layout.addLayout(self.left_layout)
        self.top_layout.addStretch()
        self.top_layout.addLayout(self.right_layout)

        card_layout.addLayout(self.top_layout)
        card_layout.addSpacing(10)

        date_layout = QVBoxLayout()
        date_layout.setSpacing(10)
        self.lbl_hijri_date = QLabel("1448 / 4 / 3 هـ")
        self.lbl_hijri_date.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.lbl_greg_date = QLabel("2026 / 9 / 14 م")
        self.lbl_greg_date.setAlignment(Qt.AlignmentFlag.AlignCenter)
        
        date_layout.addWidget(self.lbl_hijri_date)
        date_layout.addWidget(self.lbl_greg_date)
        card_layout.addLayout(date_layout)

        main_layout.addWidget(self.card)
        self.setFixedSize(450, 250)

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

    def save_position(self):
        self.settings_mgr.set("pos_x", self.x())
        self.settings_mgr.set("pos_y", self.y())


class PrayerWidget(QWidget):
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

        self.init_ui()
        self.restore_position()

    def init_ui(self):
        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)

        self.card = QFrame()
        self.card.setObjectName("prayerCard")
        self.card.setStyleSheet("QFrame#prayerCard { background-color: #232323; border-radius: 12px; }")

        card_layout = QVBoxLayout(self.card)
        card_layout.setContentsMargins(10, 20, 10, 20)
        card_layout.setSpacing(20)

        self.prayer_cards_layout = QHBoxLayout()
        self.prayer_cards_layout.setSpacing(5)
        
        self.prayer_labels = {}
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

            time_lbl = QLabel("--:--")
            time_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
            
            ampm_lbl = QLabel("ص")
            ampm_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)

            p_card_layout.addWidget(name_lbl)
            p_card_layout.addWidget(time_lbl)
            p_card_layout.addWidget(ampm_lbl)

            p_card.setStyleSheet(f"QFrame#prayer_{p_key} {{ background: transparent; border: none; border-radius: 12px; }}")

            self.prayer_cards_layout.addWidget(p_card)
            self.prayer_labels[p_key] = {"frame": p_card, "name": name_lbl, "time": time_lbl, "ampm": ampm_lbl}

        card_layout.addLayout(self.prayer_cards_layout)
        
        self.lbl_next_prayer = QLabel("بقي: --")
        self.lbl_next_prayer.setAlignment(Qt.AlignmentFlag.AlignCenter)
        card_layout.addWidget(self.lbl_next_prayer)

        main_layout.addWidget(self.card)
        self.setFixedSize(500, 220)

    def mousePressEvent(self, event):
        if event.button() == Qt.MouseButton.LeftButton:
            self.drag_position = event.globalPosition().toPoint() - self.frameGeometry().topLeft()
            event.accept()

    def mouseMoveEvent(self, event):
        if event.buttons() == Qt.MouseButton.LeftButton:
            new_pos = event.globalPosition().toPoint() - self.drag_position
            self.move(new_pos)
            self.settings_mgr.set("prayer_pos_x", new_pos.x())
            self.settings_mgr.set("prayer_pos_y", new_pos.y())
            event.accept()

    def restore_position(self):
        px = self.settings_mgr.get("prayer_pos_x", -1)
        py = self.settings_mgr.get("prayer_pos_y", -1)
        if px >= 0 and py >= 0:
            self.move(px, py)
        else:
            screen = self.screen().geometry()
            self.move(screen.width() - self.width() - 40, 320)

    def save_position(self):
        self.settings_mgr.set("prayer_pos_x", self.x())
        self.settings_mgr.set("prayer_pos_y", self.y())


class AppManager(QObject):
    def __init__(self, settings_mgr):
        super().__init__()
        self.settings_mgr = settings_mgr
        self.pray_times_calculator = PrayTimes(self.settings_mgr.get("calc_method", PrayTimes.MAKKAH))
        
        font_path = get_asset_path("tajawal.ttf")
        if os.path.exists(font_path):
            QFontDatabase.addApplicationFont(font_path)

        self.clock_widget = ClockWidget(settings_mgr)
        self.prayer_widget = PrayerWidget(settings_mgr)

        self.init_tray()
        
        # Attach context menu to widgets
        self.clock_widget.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.clock_widget.customContextMenuRequested.connect(self.show_context_menu)
        
        self.prayer_widget.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.prayer_widget.customContextMenuRequested.connect(self.show_context_menu)

        self.timer = QTimer(self)
        self.timer.timeout.connect(self.update_tick)
        self.timer.start(1000)

        self.update_all()

    def show_all(self):
        self.clock_widget.show()
        self.prayer_widget.show()

    def hide_all(self):
        self.clock_widget.hide()
        self.prayer_widget.hide()

    def toggle_visibility(self):
        if self.clock_widget.isVisible():
            self.hide_all()
        else:
            self.show_all()
            self.clock_widget.raise_()
            self.clock_widget.activateWindow()
            self.prayer_widget.raise_()
            self.prayer_widget.activateWindow()

    def init_tray(self):
        self.tray_icon = QSystemTrayIcon(self.clock_widget)
        icon_path = get_asset_path("app_icon.ico")
        if os.path.exists(icon_path):
            self.tray_icon.setIcon(QIcon(icon_path))
        
        tray_menu = QMenu()
        tray_menu.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
        tray_menu.setStyleSheet("QMenu { background-color: #1e293b; color: #ffffff; border: 1px solid #475569; border-radius: 6px; padding: 4px; font-family: 'Tajawal', sans-serif; } QMenu::item { padding: 6px 20px; border-radius: 4px; } QMenu::item:selected { background-color: #0284c7; }")

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

    def show_context_menu(self, pos):
        sender = self.sender()
        menu = QMenu(sender)
        menu.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
        menu.setStyleSheet("QMenu { background-color: #1a2233; color: #e2e8f0; border: 1px solid rgba(245, 158, 11, 0.3); border-radius: 10px; padding: 6px 4px; font-family: 'Tajawal', sans-serif; } QMenu::item { padding: 8px 24px; border-radius: 6px; } QMenu::item:selected { background-color: rgba(245, 158, 11, 0.2); color: #fbbf24; } QMenu::separator { height: 1px; background: rgba(255,255,255,0.08); margin: 4px 8px; }")

        act_calendar = QAction("📅  تقويم الشهر", self)
        act_calendar.triggered.connect(self.show_month_calendar)
        menu.addAction(act_calendar)

        act_settings = QAction("⚙️  الإعدادات", self)
        act_settings.triggered.connect(self.show_settings)
        menu.addAction(act_settings)

        menu.addSeparator()

        act_min = QAction("─  تصغير إلى شريط المهام", self)
        act_min.triggered.connect(self.hide_all)
        menu.addAction(act_min)

        act_exit = QAction("✕  إغلاق التطبيق", self)
        act_exit.triggered.connect(self.quit_app)
        menu.addAction(act_exit)

        menu.exec(sender.mapToGlobal(pos))

    def show_settings(self):
        dlg = SettingsDialog(self.settings_mgr, self.clock_widget)
        if dlg.exec():
            stay_on_top = self.settings_mgr.get("stay_on_top", False)
            flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow
            if stay_on_top:
                flags |= Qt.WindowType.WindowStaysOnTopHint
            
            self.clock_widget.setWindowFlags(flags)
            self.prayer_widget.setWindowFlags(flags)
            self.show_all()
            self.update_all()

    def show_month_calendar(self):
        dlg = MonthCalendarDialog(self.settings_mgr, self.clock_widget)
        dlg.exec()

    def update_tick(self):
        now = datetime.datetime.now()
        digit_style = self.settings_mgr.get("digit_style", 0)
        time_fmt = self.settings_mgr.get("time_format", False)
        arabic_ampm = self.settings_mgr.get("arabic_ampm", True)

        if not time_fmt:
            h = now.hour % 12
            if h == 0: h = 12
            clock_str = f"{h}:{now.minute:02d}"
            
            if arabic_ampm:
                ampm_str = "ص" if now.hour < 12 else "م"
            else:
                ampm_str = "AM" if now.hour < 12 else "PM"
        else:
            clock_str = f"{now.hour:02d}:{now.minute:02d}"
            ampm_str = ""

        self.clock_widget.lbl_clock.setText(localize_digits(clock_str, digit_style))
        self.clock_widget.lbl_ampm.setText(ampm_str)

        if now.second == 0:
            self.update_prayer_times()

    def update_all(self):
        bg_style = self.settings_mgr.get("background_style", "transparent")
        if bg_style == "transparent":
            self.clock_widget.card.setStyleSheet("QFrame#clockCard { background: transparent; }")
            self.prayer_widget.card.setStyleSheet("QFrame#prayerCard { background: transparent; }")
        else:
            self.clock_widget.card.setStyleSheet("QFrame#clockCard { background-color: #232323; border-radius: 12px; }")
            self.prayer_widget.card.setStyleSheet("QFrame#prayerCard { background-color: #232323; border-radius: 12px; }")

        # Clock Widget Layout
        c_color = self.settings_mgr.get("clock_color", "#ffffff")
        c_size_val = self.settings_mgr.get("clock_size", 2)
        c_px = {1: 48, 2: 72, 3: 96}.get(c_size_val, 72)
        ampm_px = c_px // 3
        
        self.clock_widget.lbl_clock.setStyleSheet(f"font-size: {c_px}px; font-weight: bold; color: {c_color}; font-family: 'Tajawal';")
        self.clock_widget.lbl_ampm.setStyleSheet(f"font-size: {ampm_px}px; color: {c_color}; font-family: 'Tajawal'; margin-bottom: {c_px//2.5}px;")

        h_color = self.settings_mgr.get("hijri_color", "#ffffff")
        h_size_val = self.settings_mgr.get("hijri_size", 2)
        h_px = {1: 24, 2: 36, 3: 48}.get(h_size_val, 36)
        self.clock_widget.lbl_hijri_date.setStyleSheet(f"font-size: {h_px}px; font-weight: bold; color: {h_color}; font-family: 'Tajawal';")

        g_color = self.settings_mgr.get("greg_color", "#ffffff")
        g_size_val = self.settings_mgr.get("greg_size", 2)
        g_px = {1: 24, 2: 36, 3: 48}.get(g_size_val, 36)
        self.clock_widget.lbl_greg_date.setStyleSheet(f"font-size: {g_px}px; font-weight: bold; color: {g_color}; font-family: 'Tajawal';")

        d_color = self.settings_mgr.get("day_color", "#ffffff")
        self.clock_widget.lbl_day.setStyleSheet(f"font-size: {c_px//2.5}px; font-weight: bold; color: {d_color}; font-family: 'Tajawal';")
        self.clock_widget.lbl_temp.setStyleSheet(f"font-size: {c_px//2.2}px; color: {d_color}; font-family: 'Tajawal'; margin-left: 10px; margin-bottom: 10px;")

        self.clock_widget.lbl_clock.setVisible(self.settings_mgr.get("show_clock", True))
        self.clock_widget.lbl_ampm.setVisible(self.settings_mgr.get("show_clock", True))
        self.clock_widget.lbl_hijri_date.setVisible(self.settings_mgr.get("show_hijri", True))
        self.clock_widget.lbl_greg_date.setVisible(self.settings_mgr.get("show_greg", True))
        self.clock_widget.lbl_temp.setVisible(self.settings_mgr.get("show_temp", True))

        day_left = self.settings_mgr.get("day_on_left", True)
        self.clock_widget.top_layout.removeItem(self.clock_widget.top_layout.itemAt(1))
        
        while self.clock_widget.top_layout.count():
            item = self.clock_widget.top_layout.takeAt(0)
            
        if not day_left:
            self.clock_widget.top_layout.addLayout(self.clock_widget.right_layout)
            self.clock_widget.top_layout.addStretch()
            self.clock_widget.top_layout.addLayout(self.clock_widget.left_layout)
        else:
            self.clock_widget.top_layout.addLayout(self.clock_widget.left_layout)
            self.clock_widget.top_layout.addStretch()
            self.clock_widget.top_layout.addLayout(self.clock_widget.right_layout)

        today = datetime.date.today()
        offset_days = self.settings_mgr.get("hijri_offset", 0)
        digit_style = self.settings_mgr.get("digit_style", 0)
        lang = self.settings_mgr.get("language", "ar")
        month_format = self.settings_mgr.get("month_format", "num")
        day_display = self.settings_mgr.get("day_display", "ar")

        formatted = format_full_date(today, offset_days, digit_style, lang)
        hy, hm, hd, hname = get_hijri_date(today, offset_days)
        
        h_sym = " هـ" if self.settings_mgr.get("show_hijri_symbol", True) else ""
        if month_format == "num": hijri_str = f"{hy} / {hm} / {hd}{h_sym}"
        else: hijri_str = f"{hd} {hname} {hy}{h_sym}"
            
        g_sym = " م" if self.settings_mgr.get("show_greg_symbol", True) else ""
        if month_format == "num": greg_str = f"{today.year} / {today.month} / {today.day}{g_sym}"
        else: greg_str = f"{today.day} {GREGORIAN_MONTH_NAMES_AR[today.month-1] if lang=='ar' else today.strftime('%b')} {today.year}{g_sym}"
        
        self.clock_widget.lbl_hijri_date.setText(localize_digits(hijri_str, digit_style))
        self.clock_widget.lbl_greg_date.setText(localize_digits(greg_str, digit_style))
        
        if day_display == "ar": self.clock_widget.lbl_day.setText(ARABIC_DAYS_NAMES[today.weekday()])
        else: self.clock_widget.lbl_day.setText(today.strftime('%A'))

        self.tray_icon.setToolTip(f"{formatted['day_name']} - {formatted['hijri_formatted']}")
        self.update_tick()
        self.update_prayer_times()

    def update_prayer_times(self):
        today = datetime.date.today()
        lat = self.settings_mgr.get("lat", 24.7136)
        lng = self.settings_mgr.get("lng", 46.6753)
        method_id = self.settings_mgr.get("calc_method", PrayTimes.MAKKAH)
        digit_style = self.settings_mgr.get("digit_style", 0)
        time_fmt = self.settings_mgr.get("time_format", False)
        arabic_ampm = self.settings_mgr.get("arabic_ampm", True)

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
        
        large_labels = self.settings_mgr.get("large_time_labels", False)
        lbl_size = "26px" if large_labels else "22px"

        for p_key, p_name in prayers_order:
            t_float = times.get(p_key, 0)
            if not time_fmt and not isinstance(t_float, str):
                h = int(t_float)
                m = int((t_float - h) * 60)
                is_pm = h >= 12
                if h > 12: h -= 12
                if h == 0: h = 12
                time_str = f"{h}:{m:02d}"
                if arabic_ampm: ampm_str = "م" if is_pm else "ص"
                else: ampm_str = "PM" if is_pm else "AM"
            else:
                formatted_time = self.pray_times_calculator.format_time(t_float, 0)
                time_str = formatted_time
                ampm_str = ""

            self.prayer_widget.prayer_labels[p_key]["time"].setText(localize_digits(time_str, digit_style))
            self.prayer_widget.prayer_labels[p_key]["ampm"].setText(ampm_str)
            
            self.prayer_widget.prayer_labels[p_key]["name"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
            self.prayer_widget.prayer_labels[p_key]["time"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
            self.prayer_widget.prayer_labels[p_key]["ampm"].setStyleSheet(f"font-size: 20px; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")

            if not isinstance(t_float, str) and not (isinstance(t_float, float) and (t_float != t_float)):
                p_mins = int(t_float * 60)
                
                # Check for active prayer based on user's new rule (-5 to +15 mins)
                diff_to_adhan = p_mins - current_minutes
                
                if -15 <= diff_to_adhan <= 5:
                    active_prayer_key = p_key
                    active_prayer_name = p_name
                    
                if diff_to_adhan > 0 and diff_to_adhan < next_prayer_diff:
                    next_prayer_diff = diff_to_adhan
                    next_prayer_name = p_name

        # If it's night time (after Isha)
        if next_prayer_name is None:
            # next is Fajr tomorrow
            t_fajr = times.get("Fajr", 0)
            if not isinstance(t_fajr, str):
                fajr_mins = int(t_fajr * 60) + (24*60)
                next_prayer_diff = fajr_mins - current_minutes
                next_prayer_name = "الفجر"

        # Apply styles
        for p_key, data in self.prayer_widget.prayer_labels.items():
            if p_key == active_prayer_key:
                # Highlight during the active window (-5 to +15)
                # Let's use Red for active like the user said "تحوله بي لون مختلف"
                data["frame"].setStyleSheet(f"QFrame#prayer_{p_key} {{ background-color: #f44336; border: none; border-radius: 12px; }}")
            elif p_key == next_prayer_name or (next_prayer_name == "الفجر" and p_key == "Fajr") or (next_prayer_name == "الظهر" and p_key == "Dhuhr") or (next_prayer_name == "العصر" and p_key == "Asr") or (next_prayer_name == "المغرب" and p_key == "Maghrib") or (next_prayer_name == "العشاء" and p_key == "Isha"):
                # Highlight next prayer in Yellow (#fbbf24) like the screenshot
                data["frame"].setStyleSheet(f"QFrame#prayer_{p_key} {{ background-color: #fbbf24; border: none; border-radius: 12px; }}")
                data["name"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #1e1e1e; font-family: 'Tajawal';")
                data["time"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #1e1e1e; font-family: 'Tajawal';")
                data["ampm"].setStyleSheet(f"font-size: 20px; font-weight: bold; color: #1e1e1e; font-family: 'Tajawal';")
            else:
                data["frame"].setStyleSheet(f"QFrame#prayer_{p_key} {{ background: transparent; border: none; }}")

        if active_prayer_key:
            self.prayer_widget.lbl_next_prayer.setText(f"صلاة {active_prayer_name} الآن")
            self.prayer_widget.lbl_next_prayer.setStyleSheet("font-size: 28px; font-weight: bold; color: #f44336; font-family: 'Tajawal';")
        else:
            if next_prayer_diff < 9999:
                hrs = next_prayer_diff // 60
                mins = next_prayer_diff % 60
                rem_str = ""
                if hrs > 0:
                    rem_str += f"{hrs} ساعة و "
                rem_str += f"{mins} دقيقة"
                
                self.prayer_widget.lbl_next_prayer.setText(localize_digits(f"بقي: {rem_str}", digit_style))
                self.prayer_widget.lbl_next_prayer.setStyleSheet("font-size: 28px; font-weight: bold; color: #fbbf24; font-family: 'Tajawal';")
            else:
                self.prayer_widget.lbl_next_prayer.setText("")

    def quit_app(self):
        self.clock_widget.save_position()
        self.prayer_widget.save_position()
        self.settings_mgr.save()
        self.tray_icon.hide()
        sys.exit(0)
'''

with open(r'c:/MY DATA/MY APP/hijri calendar widget/desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(base_text + new_classes)
