package com.eighthlight.fabrial.server;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpServer implements Closeable {
  // TODO: make logger instance-specific, prefixing logs w/ config & object info
  private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

  public final ServerConfig config;

  // number of clients currently connected to the server socket
  private final AtomicInteger connectionCount;

  private final ExecutorService connectionHandlerExecutor;

  private Optional<Thread> acceptThread;

  // socket which manages client connections
  // Initialized to empty, and set to a Socket object once started.
  private Optional<ServerSocket> serverSocket;

  private final ConnectionHandler handler;

  public TcpServer(ServerConfig config) {
    this(config, new HttpConnectionHandler());
  }

  public TcpServer(ServerConfig config, ConnectionHandler handler) {
    this.config = config;
    this.handler = handler;
    this.serverSocket = Optional.empty();
    this.acceptThread = Optional.empty();
    this.connectionCount = new AtomicInteger(0);
    this.connectionHandlerExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
    ServerSocket socket = new ServerSocket(config.port);
    logger.info("Server started on " + config.port);
    logger.info("Serving files from " + config.directoryPath);
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
        logger.fine("Accepted connection " + clientConnection.getRemoteSocketAddress());
        connectionHandlerExecutor.execute(() -> handleConnection(clientConnection));
      } catch (IOException e) {
        if (SocketException.class.isInstance(e) && e.getMessage().equals("Socket closed")) {
          logger.finer("Server socket closed");
        } else {
          logger.log(Level.WARNING,
                     "Exception while accepting new connection",
                     e);
        }
      }
    }
  }

  // Handle new client connections
  private void handleConnection(Socket connection) {
    this.connectionCount.incrementAndGet();

    try {
      connection.setSoTimeout(this.config.readTimeout);
    } catch (IOException e) {
      logger.log(Level.FINE,
              "Exception while configuring client connection "  + connection.getRemoteSocketAddress(),
                 e);
    }

    try (InputStream is = connection.getInputStream();
        OutputStream os = connection.getOutputStream()) {
      handler.handle(is, os);
    } catch(Throwable t) {
      logger.log(Level.WARNING, "Connection handler exception", t);
    }

    try {
      connection.close();
      logger.fine("Closed connection " + connection.getRemoteSocketAddress());
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Connection to "
                     + connection.getRemoteSocketAddress()
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
