import socket
import threading
import tkinter as tk
from tkinter import messagebox
import pyautogui

class KeyLinkServer:
    def __init__(self, root):
        self.root = root
        self.root.title("KeyLink PC Server")
        self.root.geometry("400x300")

        self.host = '0.0.0.0'
        self.port = 9999
        self.server_socket = None
        self.running = False

        self.status_label = tk.Label(root, text="Status: Stopped", fg="red", font=("Arial", 12))
        self.status_label.pack(pady=10)

        self.ip_label = tk.Label(root, text=f"IP Address: {self.get_ip()}", font=("Arial", 10))
        self.ip_label.pack(pady=5)

        self.start_button = tk.Button(root, text="Start Server", command=self.start_server, width=20, height=2)
        self.start_button.pack(pady=10)

        self.stop_button = tk.Button(root, text="Stop Server", command=self.stop_server, state=tk.DISABLED, width=20, height=2)
        self.stop_button.pack(pady=10)

        self.log_text = tk.Text(root, height=5, width=40)
        self.log_text.pack(pady=10)

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
        self.running = True
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(5)

        self.status_label.config(text="Status: Running", fg="green")
        self.start_button.config(state=tk.DISABLED)
        self.stop_button.config(state=tk.NORMAL)

        self.thread = threading.Thread(target=self.accept_connections, daemon=True)
        self.thread.start()
        self.log(f"Server started on {self.get_ip()}:{self.port}")

    def stop_server(self):
        self.running = False
        if self.server_socket:
            self.server_socket.close()
        self.status_label.config(text="Status: Stopped", fg="red")
        self.start_button.config(state=tk.NORMAL)
        self.stop_button.config(state=tk.DISABLED)
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
                data = client_socket.recv(1024).decode('utf-8')
                if not data:
                    break

                # Process commands: "KEY:a", "MOUSE:10,20", "CLICK:left"
                self.process_command(data)

            except Exception as e:
                self.log(f"Error: {e}")
                break
        client_socket.close()

    def process_command(self, command):
        try:
            if command.startswith("KEY:"):
                key = command.split(":")[1]
                pyautogui.press(key)
            elif command.startswith("MOUSE:"):
                coords = command.split(":")[1].split(",")
                dx, dy = int(coords[0]), int(coords[1])
                pyautogui.moveRel(dx, dy)
            elif command.startswith("CLICK:"):
                button = command.split(":")[1]
                pyautogui.click(button=button)
        except Exception as e:
            print(f"Command error: {e}")

if __name__ == "__main__":
    root = tk.Tk()
    app = KeyLinkServer(root)
    root.mainloop()
