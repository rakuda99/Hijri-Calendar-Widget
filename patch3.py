import re
with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

colors_list_code = '''
        self.combo_color = QComboBox()
        populate_colors(self.combo_color, self.settings_mgr.get("prayer_color", "#f8fafc"))
'''

text = re.sub(
    r'self\.combo_color = QComboBox\(\)\s*self\.combo_color\.addItem\("أبيض \(افتراضي\)", "#f8fafc"\).*?break',
    colors_list_code.strip('\n'),
    text,
    flags=re.DOTALL
)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
