import re

with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

new_init = '''
        self.main_layout = QVBoxLayout(self)
        self.scroll = QScrollArea()
        self.scroll.setWidgetResizable(True)
        self.scroll.setStyleSheet("QScrollArea { border: none; background: transparent; }")
        self.container = QWidget()
        layout = QVBoxLayout(self.container)
        self.scroll.setWidget(self.container)
        self.main_layout.addWidget(self.scroll)
'''
text = text.replace('        layout = QVBoxLayout(self)', new_init.strip('\n'), 1)
text = text.replace('        layout.addLayout(btn_box)', '        self.main_layout.addLayout(btn_box)', 1)

with open('desktop_app/widget_ui.py', 'w', encoding='utf-8') as f:
    f.write(text)
