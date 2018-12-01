package com.eighthlight.fabrial.server;

import ch.qos.logback.core.net.server.Client;
import com.bgerstle.result.Result;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.Math.*;

public class AsyncServerSocketController implements SocketController {
  private static final Logger logger =
      LoggerFactory.getLogger(AsyncServerSocketController.class);

  public final int readTimeout;

  private ServerSocket socket = null;
  private Thread acceptThread;
  private ExecutorService connectionHandlerExecutor;

  private final AtomicInteger connectionCount;

  private  int peakConnectionCount;

  public AsyncServerSocketController(int readTimeout) {
    this.readTimeout = readTimeout;
    this.connectionCount = new AtomicInteger(0);
    this.peakConnectionCount = 0;
  }

  @Override
  public int getConnectionCount() {
    return connectionCount.get();
  }

  @Override
  public int getPeakConnectionCount() {
    return peakConnectionCount;
  }

  @Override
  public void start(int port,
                    int maxConnections,
                    Consumer<ClientConnection> consumer) throws IOException {
    socket = new ServerSocket(port);

    if (maxConnections <= 0) {
      connectionHandlerExecutor = Executors.newCachedThreadPool();
    } else {
      connectionHandlerExecutor = Executors.newFixedThreadPool(maxConnections);
    }

    acceptThread = new Thread(() -> acceptConnections(consumer));
    acceptThread.setName("accept-connections");
    acceptThread.start();
  }

  @Override
  public int getPort() {
    return socket.getLocalPort();
  }

  private Socket acceptNext() throws IOException {
    var clientSocket = socket.accept();

    clientSocket.setSoTimeout(readTimeout);

    var incrementCount = this.connectionCount.incrementAndGet();
    peakConnectionCount = max(incrementCount, peakConnectionCount);

    logger.info("Accepted connection {}",
                StructuredArguments.kv("connectionCount", incrementCount));

    return clientSocket;
  }

  private void close(Socket connection) {
    var decrementCount = this.connectionCount.decrementAndGet();

    try {
      connection.close();
      logger.info("Closed connection {}",
                  StructuredArguments.kv("connectionCount", decrementCount));
    } catch (IOException e) {
      logger.warn("Error while closing connection.", e);
    }
  }

  private void acceptConnections(Consumer<ClientConnection> consumer) {
    assert socket != null;

    while (!socket.isClosed()) {
      Result.<Socket, Throwable>attempt(this::acceptNext)
          .flatMapAttempt(clientSocket -> {
            connectionHandlerExecutor.execute(() -> {
              try {
                consumer.accept(new ClientSocketConnection(clientSocket));
              } catch (Throwable t) {
                logger.warn("Connection consumer exception.", t);
              }
              close(clientSocket);
            });
            return clientSocket;
          })
          .getError()
          .ifPresent(e -> {
            if (e instanceof SocketException && e.getMessage().equals("Socket closed")) {
              logger.trace("Server socket closed");
            } else {
              logger.warn("Exception while accepting new connection", e);
            }
          });
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
