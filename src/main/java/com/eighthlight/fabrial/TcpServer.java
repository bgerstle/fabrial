package com.eighthlight.fabrial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class TcpServer implements AutoCloseable {
  private static final Logger logger = Logger.getLogger(TcpServer.class.getName());

  private final ServerConnection serverConnection;
  private final ClientConnectionHandler handler;

  public TcpServer(ServerConnection serverConnection, ClientConnectionHandler handler) {
    this.serverConnection = serverConnection;
    this.handler = handler;
  }

  public void start(int port) throws IOException {
    serverConnection
        .acceptConnections(new InetSocketAddress(port))
        .forEachRemaining(conn -> {
          try (conn) {
            handler.handle(conn);
          } catch (Exception e) {
            logger.warning("Connection exception: " + e.getMessage());
          }
        });
  }

  @Override
  public void close() throws Exception {
    serverConnection.close();
  }
}
