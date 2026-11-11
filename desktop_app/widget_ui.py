# -*- coding: utf-8 -*-
"""
واجهة المستخدم لويدجت سطح المكتب (Desktop Widget)
مبنية باستخدام PyQt6 بتصميم زجاجي عصري لسطح المكتب ودعم كامل للغة العربية.
"""

import sys
import os
import datetime
import urllib.request
import json
from PyQt6.QtCore import Qt, QPoint, QTimer, QSize, QDate, QObject, QThread, pyqtSignal
from PyQt6.QtGui import (
    QFont, QIcon, QColor, QFontDatabase, QAction, QPainter,
    QLinearGradient, QBrush, QPen, QCursor
)
from desktop_app.auto_start import set_autostart, is_autostart_enabled
import ctypes
from ctypes import wintypes

def send_to_desktop_bottom(hwnd):
    HWND_BOTTOM = 1
    SWP_NOMOVE = 0x0002
    SWP_NOSIZE = 0x0001
    SWP_NOACTIVATE = 0x0010
    ctypes.windll.user32.SetWindowPos(
        int(hwnd), HWND_BOTTOM, 0, 0, 0, 0,
        SWP_NOMOVE | SWP_NOSIZE | SWP_NOACTIVATE
    )

from PyQt6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QLabel, QPushButton,
    QFrame, QGridLayout, QDialog, QComboBox, QSpinBox,
    QCheckBox, QSlider, QSystemTrayIcon, QScrollArea, QMenu, QMessageBox,
    QGraphicsDropShadowEffect, QSizePolicy, QSpacerItem
)

from .hijri_engine import (
    get_hijri_date, get_day_name, format_full_date,
    localize_digits, HIJRI_MONTH_NAMES_AR, GREGORIAN_MONTH_NAMES_AR,
    ARABIC_DAYS_NAMES
)
from .pray_times import PrayTimes, CITIES
from .settings_manager import SettingsManager


def get_asset_path(filename):
    """الحصول على مسار الملف سواء أثناء التشغيل من بايثون أو بعد التجميع كـ EXE"""
    if getattr(sys, 'frozen', False):
        base_dir = getattr(sys, '_MEIPASS', os.path.dirname(sys.executable))
        path = os.path.join(base_dir, "assets", filename)
        if os.path.exists(path):
            return path
        path = os.path.join(base_dir, filename)
        if os.path.exists(path):
            return path
    base_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(base_dir, "assets", filename)


class WeatherWorker(QThread):
    weather_fetched = pyqtSignal(str)
    error_occurred = pyqtSignal(str)

    def __init__(self, lat=24.7136, lng=46.6753, parent=None):
        super().__init__(parent)
        self.lat = lat
        self.lng = lng

    def run(self):
        try:
            url = f"https://api.open-meteo.com/v1/forecast?latitude={self.lat}&longitude={self.lng}&current_weather=true"
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "HijriCalendarWidget/1.0"}
            )
            with urllib.request.urlopen(req, timeout=10) as response:
                if response.status == 200:
                    data = json.loads(response.read().decode('utf-8'))
                    if "current_weather" in data and "temperature" in data["current_weather"]:
                        temp = data["current_weather"]["temperature"]
                        rounded = round(float(temp))
                        temp_str = f"{rounded}°"
                        self.weather_fetched.emit(temp_str)
                        return
            self.error_occurred.emit("No temperature in response")
        except Exception as e:
            self.error_occurred.emit(str(e))



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
            QComboBox { background-color: #000000; color: #ffffff; border: 1px solid #333; border-radius: 6px; padding: 5px; min-height: 30px; font-size: 14px; }
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
        
        # City selection (Prayer & Weather)
        self.cb_city = add_combo(
            "City (Prayer & Weather) / المدينة (المواقيت والطقس)",
            [(c, c) for c in CITIES.keys()],
            "city",
            "الرياض (المملكة العربية السعودية)"
        )
        
        btn_detect = QPushButton("📍 تحديد المدينة والموقع تلقائياً عبر الإنترنت (IP)")
        btn_detect.setStyleSheet("background-color: #0284c7; color: white; padding: 7px; border-radius: 6px; font-weight: bold;")
        btn_detect.clicked.connect(self._auto_detect_location)
        layout.addWidget(btn_detect)

        btn_sync = QPushButton("🔄 مزامنة الوقت والشبكة")
        btn_sync.setStyleSheet("background-color: #f6c845; color: black; padding: 7px; border-radius: 6px; font-weight: bold;")
        btn_sync.clicked.connect(self._sync_network_time)
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
        self.chk_autostart = add_toggle("Auto-Start / تشغيل تلقائي مع بدء الويندوز", "autostart", is_autostart_enabled())
        
        self.cb_upd = add_combo("Update Interval / وقت التحديث", [("15 دقيقة", 15), ("30 دقيقة", 30), ("60 دقيقة", 60)], "update_interval", 15)

        btn_save = QPushButton("حفظ الإعدادات")
        btn_save.clicked.connect(self.save_and_close)
        layout.addWidget(btn_save)

        scroll.setWidget(container)
        main_layout.addWidget(scroll)

    def _auto_detect_location(self):
        try:
            req = urllib.request.Request("http://ip-api.com/json/", headers={"User-Agent": "HijriCalendarWidget/1.0"})
            with urllib.request.urlopen(req, timeout=5) as resp:
                data = json.loads(resp.read().decode('utf-8'))
                if data.get("status") == "success":
                    lat = float(data.get("lat"))
                    lon = float(data.get("lon"))
                    city_name = data.get("city", "")
                    self.settings_mgr.set("lat", lat)
                    self.settings_mgr.set("lng", lon)
                    
                    matched = False
                    for i in range(self.cb_city.count()):
                        item_text = self.cb_city.itemText(i)
                        if city_name.lower() in item_text.lower():
                            self.cb_city.setCurrentIndex(i)
                            matched = True
                            break
                    QMessageBox.information(
                        self, "تم بنجاح",
                        f"تم تحديد موقعك بنجاح:\nالمدينة: {city_name}\nخط العرض: {lat:.4f}\nخط الطول: {lon:.4f}"
                    )
                else:
                    QMessageBox.warning(self, "تنبيه", "تعذر تحديد الموقع الجغرافي تلقائياً.")
        except Exception as e:
            QMessageBox.warning(self, "خطأ", f"تعذر الاتصال بخدمة تحديد الموقع:\n{e}")

    def _sync_network_time(self):
        try:
            req = urllib.request.Request("https://www.google.com", method="HEAD")
            with urllib.request.urlopen(req, timeout=5) as resp:
                date_str = resp.headers.get("Date", "تم بنجاح")
                QMessageBox.information(self, "مزامنة الشبكة", f"تم الاتصال بالشبكة بنجاح.\nالوقت: {date_str}")
        except Exception as e:
            QMessageBox.warning(self, "خطأ", f"تعذر الاتصال بالشبكة:\n{e}")

    def _apply_autostart(self, enable):
        import winreg, sys, os
        key_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
        app_name = "HijriCalendarWidget"
        try:
            registry_key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, key_path, 0, winreg.KEY_WRITE)
            if enable:
                exe_path = sys.executable if getattr(sys, 'frozen', False) else os.path.abspath(sys.argv[0])
                winreg.SetValueEx(registry_key, app_name, 0, winreg.REG_SZ, f'"{exe_path}"')
            else:
                try: winreg.DeleteValue(registry_key, app_name)
                except FileNotFoundError: pass
            winreg.CloseKey(registry_key)
        except Exception as e:
            print("Autostart error:", e)

    def save_and_close(self):
        city_name = self.cb_city.currentData()
        self.settings_mgr.set("city", city_name)
        if city_name in CITIES:
            self.settings_mgr.set("lat", CITIES[city_name]["lat"])
            self.settings_mgr.set("lng", CITIES[city_name]["lng"])
            self.settings_mgr.set("calc_method", CITIES[city_name]["method"])

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
        self.settings_mgr.set("autostart", self.chk_autostart.isChecked())
        set_autostart(self.chk_autostart.isChecked())
        self.settings_mgr.set("update_interval", self.cb_upd.currentData())
        
        self.accept()


class MonthCalendarDialog(QDialog):
    """نافذة تقويم الشهر الهجري والميلادي"""
    def __init__(self, settings_mgr, parent=None):
        super().__init__(parent)
        self.settings_mgr = settings_mgr
        self.month_offset = 0
        self.setWindowTitle("تقويم الشهر v3.2")
        self.setFixedSize(540, 500)
        self.setLayoutDirection(Qt.LayoutDirection.RightToLeft)
        self.init_ui()

    def init_ui(self):
        self.setStyleSheet("""
            QDialog {
                background-color: #1a202c;
                color: #ffffff;
                font-family: 'Tajawal', 'Segoe UI', sans-serif;
            }
            QPushButton {
                background-color: #2d3748;
                color: #ffffff;
                border-radius: 8px;
                padding: 6px 14px;
                font-weight: bold;
                border: 1px solid #4a5568;
            }
            QPushButton:hover {
                background-color: #4a5568;
            }
        """)

        layout = QVBoxLayout(self)
        layout.setContentsMargins(20, 20, 20, 20)
        layout.setSpacing(14)

        # Header with navigation
        header_layout = QHBoxLayout()
        self.btn_next = QPushButton("‹ الشهر التالي")
        self.btn_next.setCursor(QCursor(Qt.CursorShape.PointingHandCursor))
        self.btn_next.clicked.connect(self.next_month)

        self.lbl_month_year = QLabel()
        self.lbl_month_year.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.lbl_month_year.setStyleSheet("font-size: 16px; font-weight: bold; color: #f59e0b;")

        self.btn_prev = QPushButton("الشهر السابق ›")
        self.btn_prev.setCursor(QCursor(Qt.CursorShape.PointingHandCursor))
        self.btn_prev.clicked.connect(self.prev_month)

        header_layout.addWidget(self.btn_prev)
        header_layout.addWidget(self.lbl_month_year)
        header_layout.addWidget(self.btn_next)
        layout.addLayout(header_layout)

        # Days Header
        days_grid = QGridLayout()
        days_grid.setSpacing(6)
        arabic_weekdays = ["السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة"]
        for col, wname in enumerate(arabic_weekdays):
            lbl = QLabel(wname)
            lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
            lbl.setStyleSheet("color: #94a3b8; font-weight: bold; font-size: 12px; padding: 4px;")
            days_grid.addWidget(lbl, 0, col)

        self.calendar_grid = days_grid
        layout.addLayout(days_grid)

        self.update_calendar()

    def prev_month(self):
        self.month_offset -= 1
        self.update_calendar()

    def next_month(self):
        self.month_offset += 1
        self.update_calendar()

    def update_calendar(self):
        # Clear old cells (keep row 0 headers)
        for i in reversed(range(self.calendar_grid.count())):
            item = self.calendar_grid.itemAt(i)
            row, col, rspan, cspan = self.calendar_grid.getItemPosition(i)
            if row > 0 and item.widget():
                item.widget().setParent(None)

        today = datetime.date.today()
        # Compute targeted year & month
        y = today.year
        m = today.month + self.month_offset
        while m > 12:
            m -= 12
            y += 1
        while m < 1:
            m += 12
            y -= 1

        first_day_g = datetime.date(y, m, 1)
        # Days in this Gregorian month
        if m == 12:
            next_m = datetime.date(y + 1, 1, 1)
        else:
            next_m = datetime.date(y, m + 1, 1)
        num_days = (next_m - first_day_g).days

        # Get Hijri of 1st and middle day for title
        offset_days = self.settings_mgr.get("hijri_offset", 0)
        digit_style = self.settings_mgr.get("digit_style", 1)
        mid_day_g = datetime.date(y, m, min(15, num_days))
        hy, hm, hd, hm_name = get_hijri_date(mid_day_g, offset_days)

        title_text = f"{hm_name} {localize_digits(hy, digit_style)} هـ  |  {GREGORIAN_MONTH_NAMES_AR[m - 1]} {localize_digits(y, digit_style)} م"
        self.lbl_month_year.setText(title_text)

        # Saturday = 5 in python weekday (Monday is 0, Sunday is 6)
        # Arabic week starts with Saturday (col 0):
        # Python weekday: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
        # Shift to Sat=0, Sun=1, Mon=2, Tue=3, Wed=4, Thu=5, Fri=6:
        def weekday_to_col(wd):
            return (wd + 2) % 7

        start_col = weekday_to_col(first_day_g.weekday())

        curr_row = 1
        curr_col = start_col

        for day_num in range(1, num_days + 1):
            day_g = datetime.date(y, m, day_num)
            h_y, h_m, h_d, _ = get_hijri_date(day_g, offset_days)

            is_today = (day_g == today)

            cell = QFrame()
            cell.setObjectName("dayCell")
            cell_layout = QVBoxLayout(cell)
            cell_layout.setContentsMargins(4, 4, 4, 4)
            cell_layout.setSpacing(2)

            # Hijri day (prominent)
            lbl_hijri = QLabel(localize_digits(h_d, digit_style))
            lbl_hijri.setAlignment(Qt.AlignmentFlag.AlignCenter)
            lbl_hijri.setStyleSheet(f"font-size: 15px; font-weight: bold; color: {'#f59e0b' if not is_today else '#ffffff'};")

            # Gregorian day (secondary)
            lbl_greg = QLabel(localize_digits(day_num, digit_style))
            lbl_greg.setAlignment(Qt.AlignmentFlag.AlignCenter)
            lbl_greg.setStyleSheet("font-size: 10px; color: #94a3b8;")

            cell_layout.addWidget(lbl_hijri)
            cell_layout.addWidget(lbl_greg)

            if is_today:
                cell.setStyleSheet("""
                    QFrame#dayCell {
                        background-color: #0284c7;
                        border: 2px solid #38bdf8;
                        border-radius: 8px;
                    }
                """)
            else:
                cell.setStyleSheet("""
                    QFrame#dayCell {
                        background-color: #242d3d;
                        border: 1px solid #334155;
                        border-radius: 8px;
                    }
                    QFrame:hover {
                        border-color: #f59e0b;
                        background-color: #2e3a4e;
                    }
                """)

            self.calendar_grid.addWidget(cell, curr_row, curr_col)

            curr_col += 1
            if curr_col > 6:
                curr_col = 0
                curr_row += 1





class ClockWidget(QWidget):
    def __init__(self, settings_mgr):
        super().__init__()
        self.settings_mgr = settings_mgr
        self.drag_position = QPoint()

        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
        if self.settings_mgr.get("stay_on_top", False):
            flags |= Qt.WindowType.WindowStaysOnTopHint
        self.setWindowFlags(flags)
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)
        self.setLayoutDirection(Qt.LayoutDirection.RightToLeft)

        self.init_ui()
        self.restore_position()

    def init_ui(self):
        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)

        self.card = QFrame()
        self.card.setObjectName("clockCard")
        self.card.setStyleSheet("QFrame#clockCard { background-color: #1c1d1f; border-radius: 12px; }")

        card_layout = QVBoxLayout(self.card)
        card_layout.setContentsMargins(20, 30, 20, 20)
        card_layout.setSpacing(20)

        self.top_layout = QHBoxLayout()
        self.top_layout.setContentsMargins(10, 0, 10, 0)
        
        self.left_layout = QHBoxLayout()
        self.lbl_day = QLabel("الاثنين")
        self.lbl_day.setAlignment(Qt.AlignmentFlag.AlignBottom | Qt.AlignmentFlag.AlignLeft)
        
        saved_temp = self.settings_mgr.get("last_temperature", "")
        initial_temp = saved_temp if saved_temp else "--°"
        digit_style = self.settings_mgr.get("digit_style", 0)
        self.lbl_temp = QLabel(localize_digits(initial_temp, digit_style))
        self.lbl_temp.setAlignment(Qt.AlignmentFlag.AlignTop | Qt.AlignmentFlag.AlignLeft)
        self.lbl_temp.setCursor(Qt.CursorShape.PointingHandCursor)
        self.lbl_temp.setToolTip("درجة الحرارة الحالية (انقر للتحديث)")
        
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

    def showEvent(self, event):
        super().showEvent(event)
        send_to_desktop_bottom(self.winId())

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

        flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
        if self.settings_mgr.get("stay_on_top", False):
            flags |= Qt.WindowType.WindowStaysOnTopHint
        self.setWindowFlags(flags)
        self.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
        self.setAttribute(Qt.WidgetAttribute.WA_TranslucentBackground)
        self.setLayoutDirection(Qt.LayoutDirection.RightToLeft)

        self.init_ui()
        self.restore_position()

    def init_ui(self):
        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)

        self.card = QFrame()
        self.card.setObjectName("prayerCard")
        self.card.setStyleSheet("QFrame#prayerCard { background-color: #1c1d1f; border-radius: 12px; }")

        card_layout = QVBoxLayout(self.card)
        card_layout.setContentsMargins(10, 10, 10, 10)
        card_layout.setSpacing(10)

        self.prayer_cards_layout = QHBoxLayout()
        self.prayer_cards_layout.setSpacing(5)
        
        self.prayer_labels = {}
        prayers = [
            ("Fajr", "الفجر"), 
            ("Dhuhr", "الجمعة" if datetime.date.today().weekday() == 4 else "الظهر"), 
            ("Asr", "العصر"),
            ("Maghrib", "المغرب"), 
            ("Isha", "العشاء")
        ]
        
        for p_key, p_name in prayers:
            p_card = QFrame()
            p_card.setObjectName(f"prayer_{p_key}")
            p_card_layout = QVBoxLayout(p_card)
            p_card_layout.setContentsMargins(10, 8, 10, 8)
            p_card_layout.setSpacing(4)

            name_lbl = QLabel(p_name)
            name_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)

            time_lbl = QLabel("--:--")
            time_lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
            
            p_card_layout.addWidget(name_lbl)
            p_card_layout.addWidget(time_lbl)

            p_card.setStyleSheet(f"QFrame#prayer_{p_key} {{ background: transparent; border: none; border-radius: 12px; }}")

            self.prayer_cards_layout.addWidget(p_card)
            self.prayer_labels[p_key] = {"frame": p_card, "name": name_lbl, "time": time_lbl}

        card_layout.addLayout(self.prayer_cards_layout)
        
        self.lbl_next_prayer = QLabel("بقي: --")
        self.lbl_next_prayer.setAlignment(Qt.AlignmentFlag.AlignCenter)
        card_layout.addWidget(self.lbl_next_prayer)

        main_layout.addWidget(self.card)
        self.setFixedSize(500, 170)

    def showEvent(self, event):
        super().showEvent(event)
        send_to_desktop_bottom(self.winId())

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

        self.weather_worker = None
        self.clock_widget.lbl_temp.mousePressEvent = lambda e: self.fetch_weather(force=True)

        self.init_tray()
        
        # Attach context menu to widgets
        self.clock_widget.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.clock_widget.customContextMenuRequested.connect(self.show_context_menu)
        
        self.prayer_widget.setContextMenuPolicy(Qt.ContextMenuPolicy.CustomContextMenu)
        self.prayer_widget.customContextMenuRequested.connect(self.show_context_menu)

        self.timer = QTimer(self)
        self.timer.timeout.connect(self.update_tick)
        self.timer.start(1000)

        # Weather update timer
        self.weather_timer = QTimer(self)
        self.weather_timer.timeout.connect(self.fetch_weather)
        interval_mins = max(5, int(self.settings_mgr.get("update_interval", 15)))
        self.weather_timer.start(interval_mins * 60 * 1000)

        self.update_all()
        self.fetch_weather(force=True)

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

        act_weather = QAction("🔄 تحديث درجة الحرارة", self)
        act_weather.triggered.connect(lambda: self.fetch_weather(force=True))
        tray_menu.addAction(act_weather)

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
        menu.setStyleSheet("QMenu { background-color: #1a2233; color: #e2e8f0; border: 1px solid rgba(245, 158, 11, 0.3); border-radius: 10px; padding: 6px 4px; font-family: 'Tajawal', sans-serif; } QMenu::item { padding: 8px 24px; border-radius: 6px; } QMenu::item:selected { background-color: rgba(245, 158, 11, 0.2); color: #f6c845; } QMenu::separator { height: 1px; background: rgba(255,255,255,0.08); margin: 4px 8px; }")

        act_weather = QAction("🔄  تحديث درجة الحرارة والطقس", self)
        act_weather.triggered.connect(lambda: self.fetch_weather(force=True))
        menu.addAction(act_weather)

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

    def fetch_weather(self, force=False):
        if not self.settings_mgr.get("show_temp", True):
            return

        if self.weather_worker is not None and self.weather_worker.isRunning():
            return

        lat = float(self.settings_mgr.get("lat", 24.7136))
        lng = float(self.settings_mgr.get("lng", 46.6753))

        self.weather_worker = WeatherWorker(lat, lng, self)
        self.weather_worker.weather_fetched.connect(self.on_weather_fetched)
        self.weather_worker.error_occurred.connect(self.on_weather_error)
        self.weather_worker.start()

    def on_weather_fetched(self, temp_str):
        self.settings_mgr.set("last_temperature", temp_str)
        digit_style = self.settings_mgr.get("digit_style", 0)
        self.clock_widget.lbl_temp.setText(localize_digits(temp_str, digit_style))
        self.weather_worker = None

    def on_weather_error(self, err_msg):
        print(f"[Weather] Error: {err_msg}")
        curr = self.clock_widget.lbl_temp.text()
        if not curr or curr in ("--", "...", "--°"):
            saved = self.settings_mgr.get("last_temperature", "")
            if saved:
                digit_style = self.settings_mgr.get("digit_style", 0)
                self.clock_widget.lbl_temp.setText(localize_digits(saved, digit_style))
            else:
                self.clock_widget.lbl_temp.setText("--°")
        self.weather_worker = None

    def show_settings(self):
        dlg = SettingsDialog(self.settings_mgr, self.clock_widget)
        if dlg.exec():
            stay_on_top = self.settings_mgr.get("stay_on_top", False)
            flags = Qt.WindowType.FramelessWindowHint | Qt.WindowType.SubWindow | Qt.WindowType.WindowStaysOnBottomHint
            if stay_on_top:
                flags |= Qt.WindowType.WindowStaysOnTopHint
            
            self.clock_widget.setWindowFlags(flags)
            self.prayer_widget.setWindowFlags(flags)
            self.clock_widget.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
            self.prayer_widget.setAttribute(Qt.WidgetAttribute.WA_ShowWithoutActivating)
            self.show_all()
            self.update_all()
            interval_mins = max(5, int(self.settings_mgr.get("update_interval", 15)))
            self.weather_timer.setInterval(interval_mins * 60 * 1000)
            self.fetch_weather(force=True)
            self.update_prayer_times()

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
            self.clock_widget.card.setStyleSheet("QFrame#clockCard { background-color: #1c1d1f; border-radius: 12px; }")
            self.prayer_widget.card.setStyleSheet("QFrame#prayerCard { background-color: #1c1d1f; border-radius: 12px; }")

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
        
        digit_style = self.settings_mgr.get("digit_style", 0)
        saved_temp = self.settings_mgr.get("last_temperature", "")
        if saved_temp:
            self.clock_widget.lbl_temp.setText(localize_digits(saved_temp, digit_style))

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
        hijri_str = f"{hy} / {hm} / {hd}{h_sym}"
            
        g_sym = " م" if self.settings_mgr.get("show_greg_symbol", True) else ""
        greg_str = f"{today.year} / {today.month} / {today.day}{g_sym}"
        
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
            ("Fajr", "الفجر"), ("Dhuhr", "الجمعة" if datetime.date.today().weekday() == 4 else "الظهر"), 
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
            else:
                formatted_time = self.pray_times_calculator.format_time(t_float, 0)
                time_str = formatted_time.split()[0]

            self.prayer_widget.prayer_labels[p_key]["time"].setText(localize_digits(time_str, digit_style))

            self.prayer_widget.prayer_labels[p_key]["name"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")
            self.prayer_widget.prayer_labels[p_key]["time"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #ffffff; font-family: 'Tajawal';")

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
                data["frame"].setStyleSheet(f"QFrame#prayer_{p_key} {{ background-color: #f6c845; border: none; border-radius: 12px; }}")
                data["name"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: \'Tajawal\';")
                data["time"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: \'Tajawal\';")
            elif p_key == next_prayer_name or (next_prayer_name == "الفجر" and p_key == "Fajr") or ((next_prayer_name == "الظهر" or next_prayer_name == "الجمعة") and p_key == "Dhuhr") or (next_prayer_name == "العصر" and p_key == "Asr") or (next_prayer_name == "المغرب" and p_key == "Maghrib") or (next_prayer_name == "العشاء" and p_key == "Isha"):
                # Highlight next prayer in Yellow (#fbbf24) like the screenshot
                data["frame"].setStyleSheet(f"QFrame#prayer_{p_key} {{ background-color: #f6c845; border: none; border-radius: 12px; }}")
                data["name"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: 'Tajawal';")
                data["time"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: 'Tajawal';")
            else:
                data["frame"].setStyleSheet(f"QFrame#prayer_{p_key} {{ background: transparent; border: none; }}")

        if active_prayer_key:
            self.prayer_widget.lbl_next_prayer.setText(f"صلاة {active_prayer_name} الآن")
            self.prayer_widget.lbl_next_prayer.setStyleSheet("font-size: 28px; font-weight: bold; color: #f6c845; font-family: 'Tajawal';")
        else:
            if next_prayer_diff < 9999:
                hrs = next_prayer_diff // 60
                mins = next_prayer_diff % 60
                rem_str = ""
                if hrs > 0:
                    rem_str += f"{hrs} ساعة و "
                rem_str += f"{mins} دقيقة"
                
                self.prayer_widget.lbl_next_prayer.setText(localize_digits(f"بقي: {rem_str}", digit_style))
                self.prayer_widget.lbl_next_prayer.setStyleSheet("font-size: 28px; font-weight: bold; color: #f6c845; font-family: 'Tajawal';")
            else:
                self.prayer_widget.lbl_next_prayer.setText("")

    def quit_app(self):
        self.clock_widget.save_position()
        self.prayer_widget.save_position()
        self.settings_mgr.save()
        self.tray_icon.hide()
        sys.exit(0)
