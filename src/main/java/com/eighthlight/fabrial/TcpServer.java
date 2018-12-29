package com.eighthlight.fabrial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class TcpServer implements AutoCloseable {
  private static final Logger logger = Logger.getLogger(TcpServer.class.getName());

  private final ServerSocket serverSocket;
  private final ClientConnectionHandler handler;

  public TcpServer(ServerSocket serverSocket, ClientConnectionHandler handler) {
    this.serverSocket = serverSocket;
    this.handler = handler;
  }

  public void start(int port) throws IOException {
    serverSocket.acceptConnections(new InetSocketAddress(port)).forEachRemaining(conn -> {
      try (conn) {
        handler.handle(conn);
      } catch (Exception e) {
        logger.warning("Connection exception: " + e.getMessage());
      }
    });
  }

  @Override
  public void close() throws Exception {
    serverSocket.close();
  }
}
