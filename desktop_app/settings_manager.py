# -*- coding: utf-8 -*-
"""
إدارة إعدادات تطبيق ويدجت سطح المكتب
يحفظ الإعدادات في ملف JSON تلقائياً في AppData أو بجانب التطبيق.
"""

import os
import json

DEFAULT_SETTINGS = {
    "city": "الرياض (المملكة العربية السعودية)",
    "lat": 24.7136,
    "lng": 46.6753,
    "calc_method": 4,  # Umm al-Qura
    "asr_juristic": 0,  # Standard Shafii/Hanbali/Maliki
    "hijri_offset": 0,  # Day correction (-2 to +2)
    "digit_style": 1,  # 1 = Eastern Arabic (٠-٩), 0 = Western (0-9)
    "time_format": 1,  # 1 = 12h, 0 = 24h
    "show_seconds": False,
    "stay_on_top": False,
    "pos_x": -1,
    "pos_y": -1,
    "theme": "glass_dark",
    "opacity": 0.95,
    "show_temp": True,
    "last_temperature": "",
    "update_interval": 15
}


class SettingsManager:
    def __init__(self):
        # Prefer AppData for persistence when packaged as EXE
        app_data = os.getenv('APPDATA')
        if app_data:
            self.config_dir = os.path.join(app_data, "HijriDesktopWidget")
        else:
            self.config_dir = os.path.dirname(os.path.abspath(__file__))
        
        try:
            os.makedirs(self.config_dir, exist_ok=True)
        except Exception:
            self.config_dir = os.path.dirname(os.path.abspath(__file__))
            
        self.config_file = os.path.join(self.config_dir, "settings.json")
        self.settings = dict(DEFAULT_SETTINGS)
        self.load()

    def load(self):
        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, "r", encoding="utf-8") as f:
                    data = json.load(f)
                    self.settings.update(data)
            except Exception as e:
                print(f"Error loading settings: {e}")

    def save(self):
        try:
            with open(self.config_file, "w", encoding="utf-8") as f:
                json.dump(self.settings, f, ensure_ascii=False, indent=2)
        except Exception as e:
            print(f"Error saving settings: {e}")

    def get(self, key, default=None):
        return self.settings.get(key, default)

    def set(self, key, value):
        self.settings[key] = value
        self.save()
