package com.eighthlight.fabrial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ServerSocketConnection implements ServerConnection {
  private static final Logger logger = Logger.getLogger(TcpServer.class.getName());

  private java.net.ServerSocket socket;

  @Override
  public Spliterator<ClientConnection> acceptConnections(InetSocketAddress address) throws IOException  {
    socket = new java.net.ServerSocket();
    socket.bind(address);
    return new ConnectionSpliterator(socket);
  }

  private static final class ConnectionSpliterator implements Spliterator<ClientConnection> {
    private final java.net.ServerSocket boundSocket;

    public ConnectionSpliterator(java.net.ServerSocket boundSocket) {
      this.boundSocket = boundSocket;
    }

    @Override
    public boolean tryAdvance(Consumer<? super ClientConnection> action) {
      try {
        var clientSocket = boundSocket.accept();
        logger.finer("Accepted connection: " + Integer.toHexString(clientSocket.getRemoteSocketAddress().hashCode()));
        action.accept(new ClientSocketConnection(clientSocket));
        return true;
      } catch (IOException e) {
        logger.info("Can't accept any more connections due to " + e.getMessage());
      }
      return false;
    }

    @Override
    public long estimateSize() {
      return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
      return 0;
    }

    @Override
    public Spliterator<ClientConnection> trySplit() {
      return null;
    }
  }

  @Override
  public void close() throws Exception {
    socket.close();
  }
}
