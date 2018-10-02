package com.eighthlight.fabrial.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AsyncServerSocketController implements SocketController {
  private static final Logger logger =
      LoggerFactory.getLogger(AsyncServerSocketController.class);

  public final int readTimeout;

  private ServerSocket socket = null;
  private Thread acceptThread;
  private final ExecutorService connectionHandlerExecutor;

  public AsyncServerSocketController(int readTimeout) {
    this.readTimeout = readTimeout;
    this.connectionHandlerExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
  }

  @Override
  public void bindServer(int port) throws IOException {
    socket = new ServerSocket(port);
  }

  @Override
  public void forEachConnection(Consumer<ClientConnection> consumer) {
    acceptThread = new Thread(() -> acceptConnections(consumer));
    acceptThread.start();
  }

  private void acceptConnections(Consumer<ClientConnection> consumer) {
    assert Thread.currentThread().equals(acceptThread);
    assert socket != null;

    while (!socket.isClosed()) {
      try {
        var clientSocket = socket.accept();
        clientSocket.setSoTimeout(readTimeout);
        var clientConnection = new ClientSocketConnection(clientSocket);
        connectionHandlerExecutor.execute(() -> consumer.accept(clientConnection));
      } catch (IOException e) {
        if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
          logger.trace("Server socket closed");
        } else {
          logger.warn("Exception while accepting new connection", e);
        }
      }
    }
  }

  @Override
  public boolean isClosed() {
    return socket.isClosed();
  }

  @Override
  public void close() throws IOException {
    if (socket ==  null) {
      logger.info("Server socket was absent, missing call to start.");
      return;
    }
    socket.close();
  }
}
