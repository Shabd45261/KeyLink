import socket

def start_server():
    server_socket = socket.socket(socket.socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 9999))
    server_socket.listen(1)
    print("Server started, waiting for connection...")

    while True:
        client_socket, addr = server_socket.accept()
        print(f"Connected to {addr}")
        while True:
            data = client_socket.recv(1024)
            if not data:
                break
            print(f"Received: {data.decode()}")
        client_socket.close()

if __name__ == "__main__":
    start_server()
