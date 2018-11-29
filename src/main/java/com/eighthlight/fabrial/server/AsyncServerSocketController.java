package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.lang.Math.*;

public class AsyncServerSocketController implements SocketController {
  private static final Logger logger =
      LoggerFactory.getLogger(AsyncServerSocketController.class);

  public final int readTimeout;

  private ServerSocket socket = null;
  private Thread acceptThread;
  private final ExecutorService connectionHandlerExecutor;

  public AsyncServerSocketController(int readTimeout) {
    this.readTimeout = readTimeout;

    var maxConnections = Optional.ofNullable(System.getProperty("maxConnections"))
                                 .flatMap(c -> Result.attempt(() -> Integer.parseInt(c)).getValue())
                                 .orElse(Runtime.getRuntime().availableProcessors());

    connectionHandlerExecutor = Executors.newFixedThreadPool(maxConnections);
  }

  public void start(int port,
                    Consumer<ClientConnection> consumer) throws IOException {
    socket = new ServerSocket(port);
    acceptThread = new Thread(() -> acceptConnections(consumer));
    acceptThread.setName("accept-connections");
    acceptThread.start();
  }

  @Override
  public int getPort() {
    return socket.getLocalPort();
  }


  private void acceptConnections(Consumer<ClientConnection> consumer) {
    assert socket != null;

    while (!socket.isClosed()) {
      try {
        var clientSocket = socket.accept();
        clientSocket.setSoTimeout(readTimeout);
        var clientConnection = new ClientSocketConnection(clientSocket);
        connectionHandlerExecutor.execute(() -> {
          try {
            consumer.accept(clientConnection);
          } catch (Throwable t) {
            logger.warn("Connection handler error", t);
          }
        });
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

    // The socket won't truly be closed until we get out of the "accept"
    // call, resulting in "address already in use" errors on subsequent
    // start/bind calls.
    try {
      acceptThread.join(10000);
    } catch (InterruptedException e) {
      logger.warn("Interrupted while awaiting accept thread termination.");
      Thread.currentThread().interrupt();
    }

    // Force executor to shut down all threads, otherwise tests will fail due to rapid accumulation
    // of threads
    connectionHandlerExecutor.shutdown();
    try {
      connectionHandlerExecutor.awaitTermination(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      logger.warn("Interrupted while awaiting connection handler thread termination, "
                  + "requesting immediate shutdown.");
      connectionHandlerExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }
}
