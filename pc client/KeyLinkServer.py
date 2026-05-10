import socket
import threading
import tkinter as tk
from tkinter import messagebox
import pyautogui

# Disable fail-safe to prevent app from stopping if mouse hits corners
pyautogui.FAILSAFE = False

class KeyLinkServer:
    def __init__(self, root):
        self.root = root
        self.root.title("KeyLink PC Server")
        self.root.geometry("400x350")
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

        self.ip_label = tk.Label(root, text=f"Local IP: {self.get_ip()}", fg="#FFFFFF", bg="#121212", font=label_font)
        self.ip_label.pack(pady=5)

        self.start_button = tk.Button(root, text="START SERVER", command=self.start_server,
                                     bg="#448AFF", fg="white", font=("Helvetica", 10, "bold"),
                                     width=20, relief=tk.FLAT, bd=0)
        self.start_button.pack(pady=10)

        self.stop_button = tk.Button(root, text="STOP SERVER", command=self.stop_server, state=tk.DISABLED,
                                    bg="#333333", fg="white", font=("Helvetica", 10, "bold"),
                                    width=20, relief=tk.FLAT, bd=0)
        self.stop_button.pack(pady=5)

        self.log_text = tk.Text(root, height=5, width=45, bg="#1E1E1E", fg="#888888", font=("Consolas", 8), bd=0)
        self.log_text.pack(pady=20, padx=10)

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

                # Split multiple commands if they arrive together
                commands = data.split('\n')
                for cmd in commands:
                    if cmd:
                        self.process_command(cmd)

            except Exception as e:
                self.log(f"Connection lost: {e}")
                break
        client_socket.close()

    def process_command(self, command):
        try:
            if command.startswith("KEY:"):
                key = command.split(":")[1].lower()
                # Special mapping for pyautogui
                special_keys = {
                    "bksp": "backspace",
                    "esc": "esc",
                    "tab": "tab",
                    "caps": "capslock",
                    "enter": "enter",
                    "shift": "shift",
                    "ctrl": "ctrl",
                    "alt": "alt",
                    "win": "win",
                    "up": "up",
                    "down": "down",
                    "left": "left",
                    "right": "right",
                    "prtscr": "printscreen",
                    "ins": "insert",
                    "del": "delete",
                    "pgup": "pageup",
                    "pgdn": "pagedown",
                    " ": "space"
                }

                if key in special_keys:
                    pyautogui.press(special_keys[key])
                elif len(key) == 1:
                    pyautogui.write(key)
                else:
                    # Fallback for F1-F12
                    pyautogui.press(key)

            elif command.startswith("MOUSE:"):
                coords = command.split(":")[1].split(",")
                dx, dy = int(coords[0]), int(coords[1])
                # Smooth the movement slightly
                pyautogui.moveRel(dx * 1.5, dy * 1.5, duration=0)

            elif command.startswith("CLICK:"):
                button = command.split(":")[1]
                pyautogui.click(button=button)
        except Exception as e:
            print(f"Command error: {e}")

if __name__ == "__main__":
    root = tk.Tk()
    app = KeyLinkServer(root)
    root.mainloop()
