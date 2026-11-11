# -*- coding: utf-8 -*-
"""
محرك حساب وتنسيق التقويم الهجري والميلادي
يدعم تقويم أم القرى، تعديل الأيام (+/-)، وأسماء الشهور، والأرقام العربية المشرقية.
"""

import math
import datetime

try:
    from hijridate import Gregorian as HijriGregorian, Hijri
    HAS_HIJRIDATE = True
except ImportError:
    HAS_HIJRIDATE = False

HIJRI_MONTH_NAMES_AR = [
    "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
    "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
    "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
]

HIJRI_MONTH_NAMES_EN = [
    "Muharram", "Safar", "Rabi' I", "Rabi' II",
    "Jumada I", "Jumada II", "Rajab", "Sha'ban",
    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
]


GREGORIAN_MONTH_NAMES_EN = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
]

ENGLISH_DAYS_NAMES = [
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
]

GREGORIAN_MONTH_NAMES_AR = [
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
]

ARABIC_DAYS_NAMES = [
    "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"
]

HINDI_DIGITS = {'0': '٠', '1': '١', '2': '٢', '3': '٣', '4': '٤', '5': '٥', '6': '٦', '7': '٧', '8': '٨', '9': '٩'}
WESTERN_DIGITS = {v: k for k, v in HINDI_DIGITS.items()}


def localize_digits(text, style=1):
    """
    style: 1 = أرقام عربية مشرقية (٠-٩)
    style: 0 = أرقام غربية (0-9)
    """
    if text is None:
        return ""
    text = str(text)
    if style == 1:
        for w, h in HINDI_DIGITS.items():
            text = text.replace(w, h)
    else:
        for h, w in WESTERN_DIGITS.items():
            text = text.replace(h, w)
    return text


def kuwaiti_gregorian_to_hijri(year, month, day):
    """الخوارزمية الفلكية الاحتياطية (الكويتية) لتحويل التاريخ الميلادي إلى هجري"""
    if month < 3:
        year -= 1
        month += 12
    a = math.floor(year / 100.0)
    b = 2 - a + math.floor(a / 4.0)
    jd = math.floor(365.25 * (year + 4716)) + math.floor(30.6001 * (month + 1)) + day + b - 1524.5
    jd = math.floor(jd) + 0.5
    z = jd - 1948440.0
    cyc = math.floor(z / 10631.0)
    z = z - 10631.0 * cyc
    j = math.floor((z - 0.133) / 354.367)
    r = z - math.floor(j * 354.367 + 0.85)
    year_h = int(30.0 * cyc + j + 1)
    month_h = int(math.floor((r + 28.5) / 29.5) + 1)
    day_h = int(r - math.floor((month_h - 1) * 29.5 + 0.5) + 30)
    if month_h > 12:
        month_h -= 12
        year_h += 1
    return year_h, month_h, day_h


def get_hijri_date(g_date=None, offset_days=0, lang="ar"):
    """
    تحويل التاريخ الميلادي إلى هجري مع تطبيق تعديل الأيام (+/-)
    يعيد (year_h, month_h, day_h, month_name_ar)
    """
    if g_date is None:
        g_date = datetime.date.today()
    
    adjusted_date = g_date + datetime.timedelta(days=offset_days)
    
    if HAS_HIJRIDATE:
        try:
            h = HijriGregorian(adjusted_date.year, adjusted_date.month, adjusted_date.day).to_hijri()
            m_name = HIJRI_MONTH_NAMES_AR[h.month - 1] if lang == "ar" else HIJRI_MONTH_NAMES_EN[h.month - 1]
            return h.year, h.month, h.day, m_name
        except Exception:
            pass

    # Fallback to Kuwaiti algorithm
    y, m, d = kuwaiti_gregorian_to_hijri(adjusted_date.year, adjusted_date.month, adjusted_date.day)
    m_name = HIJRI_MONTH_NAMES_AR[m - 1] if 1 <= m <= 12 else ""
    return y, m, d, m_name


def get_day_name(g_date=None, lang="ar"):
    if g_date is None:
        g_date = datetime.date.today()
    if lang == "en":
        return ENGLISH_DAYS_NAMES[g_date.weekday()]
    return ARABIC_DAYS_NAMES[g_date.weekday()]


def format_full_date(g_date=None, offset_days=0, digit_style=1, lang="ar"):
    """تنسيق كامل للتاريخ الهجري والميلادي واليوم"""
    if g_date is None:
        g_date = datetime.date.today()
    
    day_name = get_day_name(g_date, lang)
    hy, hm, hd, hm_name = get_hijri_date(g_date, offset_days, lang)
    
    if lang == "en":
        hijri_str = f"{hd} {hm_name} {hy} AH"
        greg_str = f"{g_date.day} {GREGORIAN_MONTH_NAMES_EN[g_date.month - 1]} {g_date.year}"
    else:
        hijri_str = f"{hd} {hm_name} {hy} هـ"
        greg_str = f"{g_date.day} {GREGORIAN_MONTH_NAMES_AR[g_date.month - 1]} {g_date.year} م"
    
    return {
        "day_name": day_name,
        "hijri_day": localize_digits(hd, digit_style),
        "hijri_month_name": hm_name,
        "hijri_year": localize_digits(hy, digit_style),
        "hijri_formatted": localize_digits(hijri_str, digit_style),
        "greg_day": localize_digits(g_date.day, digit_style),
        "greg_month_name": GREGORIAN_MONTH_NAMES_AR[g_date.month - 1] if lang == "ar" else GREGORIAN_MONTH_NAMES_EN[g_date.month - 1],
        "greg_year": localize_digits(g_date.year, digit_style),
        "greg_formatted": localize_digits(greg_str, digit_style),
    }
