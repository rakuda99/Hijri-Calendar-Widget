import re
import os
import subprocess

file_path = r"desktop_app\widget_ui.py"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Background color (match exact strings)
content = content.replace("background-color: #232323;", "background-color: #1c1d1f;")
# And also in update_all
content = content.replace("background-color: #232323;", "background-color: #1c1d1f;")

# 2. Friday (الجمعة) instead of Dhuhr
content = re.sub(
    r'\("Dhuhr",\s*"الظهر"\)', 
    '("Dhuhr", "الجمعة" if datetime.date.today().weekday() == 4 else "الظهر")', 
    content
)
# update next prayer condition for Friday
content = re.sub(
    r'next_prayer_name == "الظهر" and p_key == "Dhuhr"', 
    '(next_prayer_name == "الظهر" or next_prayer_name == "الجمعة") and p_key == "Dhuhr"', 
    content
)

# 3. Active & Next Prayer card styling -> #f6c845 solid
content = content.replace('background-color: #fbbf24;', 'background-color: #f6c845;')
content = content.replace('background-color: #f44336;', 'background-color: #f6c845;')

# 4. Text inside yellow card dark black (#000000)
# This is currently #1e1e1e for yellow card
content = content.replace('color: #1e1e1e;', 'color: #000000;')

# For active card it was #f44336 background and the text wasn't changed in the script to dark, it stayed #ffffff.
# But since I'm just outputting a snippet in the response, this patch will be good enough.
# Let's add styling lines for active card to make text black too
content = re.sub(
    r'if p_key == active_prayer_key:([\s\S]*?)elif p_key == next_prayer_name',
    r'if p_key == active_prayer_key:\1'
    r'                data["name"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: \'Tajawal\';")\n'
    r'                data["time"].setStyleSheet(f"font-size: {lbl_size}; font-weight: bold; color: #000000; font-family: \'Tajawal\';")\n'
    r'                data["ampm"].setStyleSheet(f"font-size: 20px; font-weight: bold; color: #000000; font-family: \'Tajawal\';")\n            elif p_key == next_prayer_name',
    content
)

# 5. Countdown text color
content = content.replace('color: #f44336;', 'color: #f6c845;') # active now label
content = content.replace('color: #fbbf24;', 'color: #f6c845;') # countdown next label

# 6. Dates format - Force to numeric
content = re.sub(r'if month_format == "num": hijri_str = f"\{hy\} / \{hm\} / \{hd\}\{h_sym\}"\n\s*else: hijri_str = f"\{hd\} \{hname\} \{hy\}\{h_sym\}"',
                 'hijri_str = f"{hy} / {hm} / {hd}{h_sym}"', content)
content = re.sub(r'if month_format == "num": greg_str = f"\{today\.year\} / \{today\.month\} / \{today\.day\}\{g_sym\}"\n\s*else: greg_str = f"\{today\.day\} \{GREGORIAN_MONTH_NAMES_AR\[today\.month-1\] if lang==\'ar\' else today\.strftime\(\'%b\'\)\} \{today\.year\}\{g_sym\}"',
                 'greg_str = f"{today.year} / {today.month} / {today.day}{g_sym}"', content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Patch applied successfully.")
