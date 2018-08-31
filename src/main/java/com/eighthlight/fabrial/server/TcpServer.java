package com.eighthlight.fabrial.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpServer implements Closeable {
  // TODO: make logger instance-specific, prefixing logs w/ config & object info
  private final Logger logger;

  public final ServerConfig config;

  // number of clients currently connected to the server socket
  private final AtomicInteger connectionCount;

  private final ExecutorService connectionHandlerExecutor;

  private Optional<Thread> acceptThread;

  // socket which manages client connections
  // Initialized to empty, and set to a Socket object once started.
  private Optional<ServerSocket> serverSocket;

  public TcpServer(ServerConfig config) {
    this.config = config;
    this.serverSocket = Optional.empty();
    this.acceptThread = Optional.empty();
    this.connectionCount = new AtomicInteger(0);
    this.connectionHandlerExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    this.logger = Logger.getLogger(this.toString());
  }

  public int getConnectionCount() {
    return connectionCount.get();
  }

  /**
   * Start listening on the port in `config`.
   *
   * Must not be called more than once.
   *
   * @throws IOException If the underlying socket couldn't bind successfully.
   */
  public void start() throws IOException {
    assert !serverSocket.isPresent();
    ServerSocket socket = new ServerSocket(this.config.port);
    logger.info("Server started on " + this.config.port);
    this.serverSocket = Optional.of(socket);
    Thread acceptThread = new Thread(this::acceptConnections);
    this.acceptThread = Optional.of(acceptThread);
    acceptThread.start();
  }

  private void acceptConnections() {
    assert acceptThread.isPresent();
    assert Thread.currentThread().equals(acceptThread.get());
    assert serverSocket.isPresent();

    ServerSocket serverSocket = this.serverSocket.get();
    while (!serverSocket.isClosed()) {
      try {
        Socket clientConnection = serverSocket.accept();
        connectionHandlerExecutor.execute(() -> handleConnection(clientConnection));
      } catch (IOException e) {
        logger.log(Level.FINE,
                   "Exception while accepting new connection",
                   e);
      }
    }
  }

  // Handle new client connections
  private void handleConnection(Socket connection) {
    this.connectionCount.incrementAndGet();
    try {
      connection.setSoTimeout(this.config.readTimeout);
      try (InputStream is = connection.getInputStream();
          OutputStream os = connection.getOutputStream()) {
        is.transferTo(os);
      }

    } catch (IOException e) {
      logger.log(Level.FINE,
              "Exception while handling connection to "  + connection.getInetAddress(),
                 e);
    }

    try {
      connection.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Connection to "
                     + connection.getInetAddress()
                     + " encountered exception while closing",
                 e);
    }
    this.connectionCount.decrementAndGet();
  }

  /**
   * @return Whether or not the underlying socket is closed.
   */
  public boolean isClosed() {
    return serverSocket.map(ServerSocket::isClosed).orElse(true);
  }

  public void close() throws IOException {
    logger.info("Closing...");
    if (serverSocket.isPresent()) {
      serverSocket.get().close();
      assert acceptThread.isPresent();
      try {
        acceptThread.get().join();
      } catch (InterruptedException e) {
        logger.warning("Failed to wait for accept thread to join.");
      }
    } else {
      logger.info("Server socket was absent, missing call to start.");
    }
  }
}
