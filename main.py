import sys
import os

base_dir = getattr(sys, '_MEIPASS', os.path.dirname(os.path.abspath(__file__)))
if base_dir not in sys.path:
    sys.path.insert(0, base_dir)

# Add parent directory as well
parent_dir = os.path.dirname(base_dir)
if parent_dir not in sys.path:
    sys.path.insert(0, parent_dir)

from desktop_app.main import main

if __name__ == "__main__":
    main()
