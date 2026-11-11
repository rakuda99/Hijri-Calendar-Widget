import os
import sys
import winreg

REG_PATH = r"Software\Microsoft\Windows\CurrentVersion\Run"
APP_NAME = "HijriCalendarWidget"

def get_executable_path():
    if getattr(sys, 'frozen', False):
        return sys.executable
    return os.path.abspath(__file__)

def set_autostart(enable: bool = True):
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, REG_PATH, 0, winreg.KEY_SET_VALUE)
        if enable:
            exe_path = f'"{get_executable_path()}"'
            winreg.SetValueEx(key, APP_NAME, 0, winreg.REG_SZ, exe_path)
        else:
            try:
                winreg.DeleteValue(key, APP_NAME)
            except FileNotFoundError:
                pass
        winreg.CloseKey(key)
        return True
    except Exception as e:
        print(f"Error setting autostart: {e}")
        return False

def is_autostart_enabled() -> bool:
    try:
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, REG_PATH, 0, winreg.KEY_READ)
        winreg.QueryValueEx(key, APP_NAME)
        winreg.CloseKey(key)
        return True
    except FileNotFoundError:
        return False
