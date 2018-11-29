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

import static java.lang.Math.max;
import static java.lang.Math.min;

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

    var coreSize = min(Runtime.getRuntime().availableProcessors(), maxConnections);
    var maxSize = max(coreSize, maxConnections);

    this.connectionHandlerExecutor =
        new ThreadPoolExecutor(coreSize,
                               maxSize,
                               10,
                               TimeUnit.SECONDS,
                               new SynchronousQueue<>());
  }

  public void start(int port,
                    Consumer<ClientConnection> consumer) throws IOException {
    socket = new ServerSocket(port);
    acceptThread = new Thread(() -> acceptConnections(consumer));
    acceptThread.setName("accept-connections");
    acceptThread.setPriority(Thread.MAX_PRIORITY);
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
        connectionHandlerExecutor.execute(() -> {
          assert !Thread.currentThread().equals(acceptThread);
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
      Thread.currentThread().interrupt();
    }
  }
}
