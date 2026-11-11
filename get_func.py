with open('desktop_app/widget_ui.py', 'r', encoding='utf-8') as f:
    text = f.read()

import ast

class FuncVisitor(ast.NodeVisitor):
    def visit_FunctionDef(self, node):
        if node.name == 'update_prayer_times':
            lines = text.split('\n')[node.lineno-1:node.end_lineno]
            print('\n'.join(lines))

tree = ast.parse(text)
visitor = FuncVisitor()
visitor.visit(tree)
