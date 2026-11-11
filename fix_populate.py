import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Remove the def populate_colors from its current place
def_pattern = r'        def populate_colors\(combo, current_val\):.*?                    break\n'
text = re.sub(def_pattern, '', text, flags=re.DOTALL)

# 2. Add it at the beginning of init_ui
top_code = '''    def init_ui(self):
        def populate_colors(combo, current_val):
            combo.clear()
            colors = [("أزرق", "#38bdf8"), ("ذهبي", "#fbbf24"), ("أخضر", "#4ade80"), ("أبيض", "#f8fafc"), ("رمادي", "#94a3b8")]
            for name, val in colors:
                combo.addItem(name, val)
            for i in range(combo.count()):
                if combo.itemData(i) == current_val:
                    combo.setCurrentIndex(i)
                    break
'''
text = text.replace('    def init_ui(self):', top_code, 1)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
