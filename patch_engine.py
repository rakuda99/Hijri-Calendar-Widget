import re

with open('desktop_app/hijri_engine.py', 'r', encoding='utf-8') as f:
    text = f.read()

en_arrays = '''
GREGORIAN_MONTH_NAMES_EN = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
]

ENGLISH_DAYS_NAMES = [
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
]
'''
if 'GREGORIAN_MONTH_NAMES_EN' not in text:
    text = text.replace('GREGORIAN_MONTH_NAMES_AR = [', en_arrays + '\nGREGORIAN_MONTH_NAMES_AR = [')

# Update get_day_name
text = re.sub(
    r'def get_day_name\(g_date=None\):.*?return ARABIC_DAYS_NAMES\[g_date\.weekday\(\)\]',
    r'''def get_day_name(g_date=None, lang="ar"):
    if g_date is None:
        g_date = datetime.date.today()
    if lang == "en":
        return ENGLISH_DAYS_NAMES[g_date.weekday()]
    return ARABIC_DAYS_NAMES[g_date.weekday()]''',
    text, flags=re.DOTALL
)

# Update get_hijri_date
text = re.sub(
    r'def get_hijri_date\(g_date=None, offset_days=0\):',
    r'def get_hijri_date(g_date=None, offset_days=0, lang="ar"):',
    text
)
text = text.replace(
    'm_name = HIJRI_MONTH_NAMES_AR[h.month - 1]',
    'm_name = HIJRI_MONTH_NAMES_AR[h.month - 1] if lang == "ar" else HIJRI_MONTH_NAMES_EN[h.month - 1]'
)
text = text.replace(
    'm_name = HIJRI_MONTH_NAMES_AR[um_m - 1]',
    'm_name = HIJRI_MONTH_NAMES_AR[um_m - 1] if lang == "ar" else HIJRI_MONTH_NAMES_EN[um_m - 1]'
)

# Update format_full_date
text = re.sub(
    r'def format_full_date\(g_date=None, offset_days=0, digit_style=1\):',
    r'def format_full_date(g_date=None, offset_days=0, digit_style=1, lang="ar"):',
    text
)
new_format = '''    day_name = get_day_name(g_date, lang)
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
    }'''
text = re.sub(r'    day_name = get_day_name\(g_date\).*?greg_formatted": localize_digits\(greg_str, digit_style\),\n    \}', new_format, text, flags=re.DOTALL)

with open('desktop_app/hijri_engine.py', 'w', encoding='utf-8') as f:
    f.write(text)
