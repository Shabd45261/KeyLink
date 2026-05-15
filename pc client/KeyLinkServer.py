import socket
import threading
import tkinter as tk
from tkinter import messagebox
import pyautogui
import ctypes

# Windows Mouse Event Constants
MOUSEEVENTF_MOVE = 0x0001
MOUSEEVENTF_LEFTDOWN = 0x0002
MOUSEEVENTF_LEFTUP = 0x0004
MOUSEEVENTF_RIGHTDOWN = 0x0008
MOUSEEVENTF_RIGHTUP = 0x0010
MOUSEEVENTF_MIDDLEDOWN = 0x0020
MOUSEEVENTF_MIDDLEUP = 0x0040

def win_move_mouse(dx, dy):
    ctypes.windll.user32.mouse_event(MOUSEEVENTF_MOVE, int(dx), int(dy), 0, 0)

def win_click_mouse(button, action):
    if button == "left":
        flag = MOUSEEVENTF_LEFTDOWN if action == "down" else MOUSEEVENTF_LEFTUP
    elif button == "right":
        flag = MOUSEEVENTF_RIGHTDOWN if action == "down" else MOUSEEVENTF_RIGHTUP
    elif button == "middle":
        flag = MOUSEEVENTF_MIDDLEDOWN if action == "down" else MOUSEEVENTF_MIDDLEUP
    else: return
    ctypes.windll.user32.mouse_event(flag, 0, 0, 0, 0)

# Disable fail-safe and reduce pause
pyautogui.FAILSAFE = False
pyautogui.PAUSE = 0

class KeyLinkServer:
    def __init__(self, root):
        self.root = root
        self.root.title("KeyLink PC Server")
        self.root.geometry("400x400")
        self.root.configure(bg="#121212")

        self.host = '0.0.0.0'
        self.port = 9999
        self.server_socket = None
        self.running = False

        # UI Styling
        title_font = ("Helvetica", 16, "bold")
        label_font = ("Helvetica", 10)

        tk.Label(root, text="KeyLink Server", fg="#448AFF", bg="#121212", font=title_font).pack(pady=20)

        self.status_label = tk.Label(root, text="Status: Stopped", fg="#FF5252", bg="#121212", font=label_font)
        self.status_label.pack(pady=5)

        self.ip_label = tk.Label(root, text=f"Local IP: {self.get_ip()}\n(Use this IP in Android App)", fg="#FFFFFF", bg="#121212", font=label_font)
        self.ip_label.pack(pady=5)

        tk.Label(root, text="Make sure PC and Android are on same WiFi\nand Firewall allows port 9999", fg="#888888", bg="#121212", font=("Helvetica", 8)).pack(pady=5)

        self.start_button = tk.Button(root, text="START SERVER", command=self.start_server,
                                     bg="#448AFF", fg="white", font=("Helvetica", 10, "bold"),
                                     width=20, relief=tk.FLAT, bd=0)
        self.start_button.pack(pady=10)

        self.stop_button = tk.Button(root, text="STOP SERVER", command=self.stop_server, state=tk.DISABLED,
                                    bg="#333333", fg="white", font=("Helvetica", 10, "bold"),
                                    width=20, relief=tk.FLAT, bd=0)
        self.stop_button.pack(pady=5)

        self.log_text = tk.Text(root, height=8, width=45, bg="#1E1E1E", fg="#888888", font=("Consolas", 8), bd=0)
        self.log_text.pack(pady=10, padx=10)

    def get_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"

    def log(self, message):
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)

    def start_server(self):
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            self.running = True

            self.status_label.config(text="Status: Running (Live)", fg="#4CAF50")
            self.start_button.config(state=tk.DISABLED, bg="#222222")
            self.stop_button.config(state=tk.NORMAL, bg="#FF5252")

            self.thread = threading.Thread(target=self.accept_connections, daemon=True)
            self.thread.start()
            self.log(f"Server started on {self.get_ip()}:{self.port}")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to start server: {e}")

    def stop_server(self):
        self.running = False
        if self.server_socket:
            self.server_socket.close()
        self.status_label.config(text="Status: Stopped", fg="#FF5252")
        self.start_button.config(state=tk.NORMAL, bg="#448AFF")
        self.stop_button.config(state=tk.DISABLED, bg="#333333")
        self.log("Server stopped.")

    def accept_connections(self):
        while self.running:
            try:
                client_socket, addr = self.server_socket.accept()
                self.log(f"Connected by {addr}")
                handle_thread = threading.Thread(target=self.handle_client, args=(client_socket,), daemon=True)
                handle_thread.start()
            except Exception:
                break

    def handle_client(self, client_socket):
        while self.running:
            try:
                data = client_socket.recv(1024).decode('utf-8').strip()
                if not data:
                    break
                commands = data.split('\n')
                for cmd in commands:
                    clean_cmd = cmd.strip()
                    if clean_cmd:
                        self.process_command(clean_cmd)
            except Exception:
                break
        client_socket.close()

    def process_command(self, command):
        try:
            if command.startswith("CONNECTED:"):
                return

            if command.startswith("DOWN:") or command.startswith("UP:"):
                parts = command.split(":")
                action, key = parts[0], parts[1].lower()
                special_keys = {
                    "bksp": "backspace", "backspace": "backspace", "esc": "esc", "tab": "tab",
                    "caps": "capslock", "capslock": "capslock", "enter": "enter", "shift": "shift",
                    "ctrl": "ctrl", "alt": "alt", "win": "win", "up": "up", "down": "down",
                    "left": "left", "right": "right", "prtsc": "printscreen", "ins": "insert",
                    "del": "delete", "pgup": "pageup", "pgdn": "pagedown", "space": "space"
                }
                target_key = special_keys.get(key, key)
                if action == "DOWN":
                    pyautogui.keyDown(target_key)
                else:
                    pyautogui.keyUp(target_key)

            elif command.startswith("MOUSE:"):
                coords = command.split(":")[1].split(",")
                win_move_mouse(int(coords[0]), int(coords[1]))

            elif command.startswith("SCROLL:"):
                coords = command.split(":")[1].split(",")
                pyautogui.scroll(int(coords[1]) * -3)
                pyautogui.hscroll(int(coords[0]) * 3)

            elif command.startswith("ZOOM:"):
                factor = float(command.split(":")[1])
                if factor > 1.05: pyautogui.hotkey('ctrl', '+')
                elif factor < 0.95: pyautogui.hotkey('ctrl', '-')

            elif command.startswith("DRAG:"):
                coords = command.split(":")[1].split(",")
                win_move_mouse(int(coords[0]), int(coords[1]))

            elif command.startswith("DRAG_START"):
                win_click_mouse("left", "down")

            elif command.startswith("DRAG_RELEASE"):
                win_click_mouse("left", "up")

            elif command.startswith("CLICK:"):
                btn = command.split(":")[1]
                win_click_mouse(btn, "down")
                win_click_mouse(btn, "up")

            elif command.startswith("GESTURE:"):
                gesture = command.split(":")[1]
                if gesture == "3_UP": pyautogui.hotkey('win', 'tab')
                elif gesture == "3_DOWN": pyautogui.hotkey('win', 'd')
                elif gesture == "3_LEFT": pyautogui.hotkey('alt', 'shift', 'tab')
                elif gesture == "3_RIGHT": pyautogui.hotkey('alt', 'tab')
                elif gesture == "4_UP": pyautogui.hotkey('win', 'tab')
                elif gesture == "4_DOWN": pyautogui.hotkey('win', 'd')
                elif gesture == "4_LEFT": pyautogui.hotkey('ctrl', 'win', 'left')
                elif gesture == "4_RIGHT": pyautogui.hotkey('ctrl', 'win', 'right')
                elif gesture == "4_TAP": pyautogui.hotkey('win', 'a')

        except Exception as e:
            print(f"Error: {e}")

if __name__ == "__main__":
    root = tk.Tk()
    app = KeyLinkServer(root)
    root.mainloop()
