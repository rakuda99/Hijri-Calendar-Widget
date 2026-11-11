# -*- coding: utf-8 -*-
"""
محرك حساب مواقيت الصلاة
مبني على خوارزمية PrayTimes.org الرسمية المستخدمة في التطبيق الأصلي.
"""

import math
import datetime
import time


class PrayTimes:
    # Calculation Methods
    JAFARI = 0   # Ithna Ashari
    KARACHI = 1  # University of Islamic Sciences, Karachi
    ISNA = 2     # Islamic Society of North America
    MWL = 3      # Muslim World League
    MAKKAH = 4   # Umm al-Qura, Makkah
    EGYPT = 5    # Egyptian General Authority of Survey
    TEHRAN = 6   # Institute of Geophysics, University of Tehran

    # Juristic Methods (Asr)
    SHAFII = 0   # Standard (Shafii, Maliki, Hanbali)
    HANAFI = 1   # Hanafi

    # Adjusting Methods for Higher Latitudes
    NONE = 0
    MIDNIGHT = 1
    ONE_SEVENTH = 2
    ANGLE_BASED = 3

    # Time Formats
    TIME_24 = 0
    TIME_12 = 1

    time_names = ['Fajr', 'Sunrise', 'Dhuhr', 'Asr', 'Sunset', 'Maghrib', 'Isha']
    arabic_names = {
        'Fajr': 'الفجر',
        'Sunrise': 'الشروق',
        'Dhuhr': 'الظهر',
        'Asr': 'العصر',
        'Sunset': 'الغروب',
        'Maghrib': 'المغرب',
        'Isha': 'العشاء'
    }

    def __init__(self, method_id=MAKKAH):
        self.calc_method = method_id
        self.asr_juristic = self.SHAFII
        self.dhuhr_minutes = 0
        self.adjust_high_lats = self.MIDNIGHT
        self.time_format = self.TIME_12

        # Parameters: [fajr_angle, maghrib_is_minutes, maghrib_val, isha_is_minutes, isha_val]
        self.method_params = {
            self.JAFARI:  [16.0, 0, 4.0, 0, 14.0],
            self.KARACHI: [18.0, 1, 0.0, 0, 18.0],
            self.ISNA:    [15.0, 1, 0.0, 0, 15.0],
            self.MWL:     [18.0, 1, 0.0, 0, 17.0],
            self.MAKKAH:  [18.5, 1, 0.0, 1, 90.0],
            self.EGYPT:   [19.5, 1, 0.0, 0, 17.5],
            self.TEHRAN:  [17.7, 0, 4.5, 0, 14.0],
        }
        self.offsets = [0, 0, 0, 0, 0, 0, 0]

    # Trigonometric functions in degrees
    @staticmethod
    def fix_angle(a):
        return a - 360.0 * math.floor(a / 360.0)

    @staticmethod
    def fix_hour(a):
        return a - 24.0 * math.floor(a / 24.0)

    @staticmethod
    def dsin(d):
        return math.sin(math.radians(d))

    @staticmethod
    def dcos(d):
        return math.cos(math.radians(d))

    @staticmethod
    def dtan(d):
        return math.tan(math.radians(d))

    @staticmethod
    def darcsin(x):
        return math.degrees(math.asin(max(-1.0, min(1.0, x))))

    @staticmethod
    def darccos(x):
        return math.degrees(math.acos(max(-1.0, min(1.0, x))))

    @staticmethod
    def darctan2(y, x):
        return math.degrees(math.atan2(y, x))

    @staticmethod
    def darccot(x):
        return math.degrees(math.atan2(1.0, x))

    # Julian Date calculation
    @staticmethod
    def julian_date(year, month, day):
        if month <= 2:
            year -= 1
            month += 12
        a = math.floor(year / 100.0)
        b = 2 - a + math.floor(a / 4.0)
        return math.floor(365.25 * (year + 4716)) + math.floor(30.6001 * (month + 1)) + day + b - 1524.5

    def sun_position(self, jd):
        d = jd - 2451545.0
        g = self.fix_angle(357.529 + 0.98560028 * d)
        q = self.fix_angle(280.459 + 0.98564736 * d)
        l = self.fix_angle(q + 1.915 * self.dsin(g) + 0.020 * self.dsin(2 * g))
        e = 23.439 - 0.00000036 * d
        dd = self.darcsin(self.dsin(e) * self.dsin(l))
        ra = self.darctan2(self.dcos(e) * self.dsin(l), self.dcos(l)) / 15.0
        ra = self.fix_hour(ra)
        eq_t = q / 15.0 - ra
        return dd, eq_t

    def compute_mid_day(self, eq_t):
        return self.fix_hour(12 - eq_t)

    def compute_time(self, g, t, lat, decl):
        d = -self.dsin(g) - self.dsin(lat) * self.dsin(decl)
        n = self.dcos(lat) * self.dcos(decl)
        if n == 0 or abs(d / n) > 1.0:
            return float('nan')
        v = 1.0 / 15.0 * self.darccos(d / n)
        return t + (-v if g > 90 else v)

    def compute_asr(self, step, t, lat, decl):
        d = -self.darccot(step + self.dtan(abs(lat - decl)))
        n = -self.dsin(d) - self.dsin(lat) * self.dsin(decl)
        denom = self.dcos(lat) * self.dcos(decl)
        if denom == 0 or abs(n / denom) > 1.0:
            return float('nan')
        v = 1.0 / 15.0 * self.darccos(n / denom)
        return t + v

    def get_times(self, date, lat, lng, timezone_offset=None):
        if timezone_offset is None:
            # Auto detect system timezone offset in hours
            now = time.time()
            local_time = datetime.datetime.fromtimestamp(now)
            utc_time = datetime.datetime.fromtimestamp(now, datetime.timezone.utc).replace(tzinfo=None)
            timezone_offset = (local_time - utc_time).total_seconds() / 3600.0

        jd = self.julian_date(date.year, date.month, date.day) - lng / (15.0 * 24.0)
        decl, eq_t = self.sun_position(jd)
        noon = self.compute_mid_day(eq_t)

        params = self.method_params.get(self.calc_method, self.method_params[self.MAKKAH])
        fajr_angle = params[0]
        maghrib_is_min = params[1]
        maghrib_val = params[2]
        isha_is_min = params[3]
        isha_val = params[4]

        fajr = self.compute_time(180 - fajr_angle, noon, lat, decl)
        sunrise = self.compute_time(180 - 0.833, noon, lat, decl)
        dhuhr = noon + (self.dhuhr_minutes / 60.0)
        asr_step = 2 if self.asr_juristic == self.HANAFI else 1
        asr = self.compute_asr(asr_step, noon, lat, decl)
        sunset = self.compute_time(0.833, noon, lat, decl)

        if maghrib_is_min == 1:
            maghrib = sunset + (maghrib_val / 60.0)
        else:
            maghrib = self.compute_time(maghrib_val, noon, lat, decl)

        if isha_is_min == 1:
            isha = maghrib + (isha_val / 60.0)
        else:
            isha = self.compute_time(isha_val, noon, lat, decl)

        times = [fajr, sunrise, dhuhr, asr, sunset, maghrib, isha]

        # Convert to local time
        results = {}
        for i, name in enumerate(self.time_names):
            t = times[i]
            if math.isnan(t):
                results[name] = "--:--"
            else:
                t = t + timezone_offset - (lng / 15.0)
                t = self.fix_hour(t) + (self.offsets[i] / 60.0)
                results[name] = t

        return results

    def format_time(self, time_float, time_format=TIME_12, arabic_suffix=True):
        if math.isnan(time_float) or isinstance(time_float, str):
            return "--:--"
        
        hours = int(math.floor(time_float))
        minutes = int(math.floor((time_float - hours) * 60 + 0.5))
        if minutes >= 60:
            hours += 1
            minutes -= 60
        hours = hours % 24

        if time_format == self.TIME_24:
            return f"{hours:02d}:{minutes:02d}"
        else:
            period = "ص" if hours < 12 else "م"
            if not arabic_suffix:
                period = "AM" if hours < 12 else "PM"
            h12 = hours % 12
            if h12 == 0:
                h12 = 12
            return f"{h12:02d}:{minutes:02d} {period}"


# Cities database with latitude and longitude
CITIES = {
    "الرياض (المملكة العربية السعودية)": {"lat": 24.7136, "lng": 46.6753, "method": PrayTimes.MAKKAH},
    "مكة المكرمة (المملكة العربية السعودية)": {"lat": 21.3891, "lng": 39.8579, "method": PrayTimes.MAKKAH},
    "المدينة المنورة (المملكة العربية السعودية)": {"lat": 24.5247, "lng": 39.5692, "method": PrayTimes.MAKKAH},
    "جدة (المملكة العربية السعودية)": {"lat": 21.5433, "lng": 39.1728, "method": PrayTimes.MAKKAH},
    "الدمام (المملكة العربية السعودية)": {"lat": 26.4207, "lng": 50.0888, "method": PrayTimes.MAKKAH},
    "القاهرة (مصر)": {"lat": 30.0444, "lng": 31.2357, "method": PrayTimes.EGYPT},
    "الإسكندرية (مصر)": {"lat": 31.2001, "lng": 29.9187, "method": PrayTimes.EGYPT},
    "دبي (الإمارات العربية المتحدة)": {"lat": 25.2048, "lng": 55.2708, "method": PrayTimes.MAKKAH},
    "أبوظبي (الإمارات العربية المتحدة)": {"lat": 24.4539, "lng": 54.3773, "method": PrayTimes.MAKKAH},
    "الدوحة (قطر)": {"lat": 25.2854, "lng": 51.5310, "method": PrayTimes.MAKKAH},
    "مدينة الكويت (الكويت)": {"lat": 29.3759, "lng": 47.9774, "method": PrayTimes.MAKKAH},
    "المنامة (البحرين)": {"lat": 26.2285, "lng": 50.5860, "method": PrayTimes.MAKKAH},
    "مسقط (عُمان)": {"lat": 23.5880, "lng": 58.3829, "method": PrayTimes.MAKKAH},
    "عمان (الأردن)": {"lat": 31.9454, "lng": 35.9284, "method": PrayTimes.MWL},
    "القدس الشريف (فلسطين)": {"lat": 31.7683, "lng": 35.2137, "method": PrayTimes.MWL},
    "دمشق (سوريا)": {"lat": 33.5138, "lng": 36.2765, "method": PrayTimes.MWL},
    "بيروت (لبنان)": {"lat": 33.8938, "lng": 35.5018, "method": PrayTimes.MWL},
    "بغداد (العراق)": {"lat": 33.3152, "lng": 44.3661, "method": PrayTimes.MWL},
    "صنعاء (اليمن)": {"lat": 15.3694, "lng": 44.1910, "method": PrayTimes.MAKKAH},
    "الخرطوم (السودان)": {"lat": 15.5007, "lng": 32.5599, "method": PrayTimes.EGYPT},
    "طرابلس (ليبيا)": {"lat": 32.8872, "lng": 13.1913, "method": PrayTimes.MWL},
    "تونس (تونس)": {"lat": 36.8065, "lng": 10.1815, "method": PrayTimes.MWL},
    "الجزائر (الجزائر)": {"lat": 36.7538, "lng": 3.0588, "method": PrayTimes.MWL},
    "الرباط (المغرب)": {"lat": 34.0209, "lng": -6.8416, "method": PrayTimes.MWL},
    "إسطنبول (تركيا)": {"lat": 41.0082, "lng": 28.9784, "method": PrayTimes.MWL},
    "لندن (المملكة المتحدة)": {"lat": 51.5074, "lng": -0.1278, "method": PrayTimes.MWL},
    "باريس (فرنسا)": {"lat": 48.8566, "lng": 2.3522, "method": PrayTimes.MWL},
    "نيويورك (الولايات المتحدة)": {"lat": 40.7128, "lng": -74.0060, "method": PrayTimes.ISNA},
}
