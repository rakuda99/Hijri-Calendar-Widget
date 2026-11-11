import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

clock_code = '''
        show_clock = self.settings_mgr.get("show_clock", True)
        self.lbl_time.setVisible(show_clock)
        self.lbl_colon.setVisible(show_clock)
        self.lbl_ampm.setVisible(show_clock)
'''

text = re.sub(
    r'        # Update time.*?self\.lbl_ampm\.setText\(ampm_str\)',
    r'\g<0>' + '\n' + clock_code,
    text,
    flags=re.DOTALL
)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
