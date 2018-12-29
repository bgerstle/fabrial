package com.eighthlight.fabrial;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TcpServer {
  private final ServerSocket serverSocket;
  private final ClientConnectionHandler handler;

  public TcpServer(ServerSocket serverSocket, ClientConnectionHandler handler) {
    this.serverSocket = serverSocket;
    this.handler = handler;
  }

  public void start(int port) throws IOException {
    serverSocket.acceptConnections(new InetSocketAddress(port)).forEachRemaining(handler::handle);
  }
}
