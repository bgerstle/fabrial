package com.eighthlight.fabrial;

public class TcpServer {
  private final ServerSocket serverSocket;
  private final ClientConnectionHandler handler;

  public TcpServer(ServerSocket serverSocket, ClientConnectionHandler handler) {
    this.serverSocket = serverSocket;
    this.handler = handler;
  }

  public void start(int port) {
    serverSocket.acceptConnections(port).forEach(handler::handle);
  }
}
