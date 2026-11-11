import ast
with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()
class V(ast.NodeVisitor):
    def visit_FunctionDef(self, node):
        if node.name == 'init_ui' and getattr(self, 'in_settings', False):
            lines = text.split('\n')[node.lineno-1:node.end_lineno]
            print('\n'.join(lines))
    def visit_ClassDef(self, node):
        if node.name == 'SettingsDialog':
            self.in_settings = True
            self.generic_visit(node)
            self.in_settings = False
v = V()
v.visit(ast.parse(text))
