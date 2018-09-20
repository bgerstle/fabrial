package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.file.FileResponderFileControllerImpl;
import com.eighthlight.fabrial.http.HttpConnectionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
    this(config,
         new HttpConnectionHandler(
             Set.of(
                 new FileHttpResponder(
                     new FileResponderFileControllerImpl(config.directoryPath)
                 ))));
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
        connectionHandlerExecutor.execute(() -> handleConnection(clientConnection));
      } catch (IOException e) {
        if (SocketException.class.isInstance(e) && e.getMessage().equals("Socket closed")) {
          logger.trace("Server socket closed");
        } else {
          logger.warn("Exception while accepting new connection",
                     e);
        }
      }
    }
  }

  // Handle new client connections
  private void handleConnection(Socket connection) {
    try (MDC.MDCCloseable cra = MDC.putCloseable("connectionId",
                                                 Integer.toHexString(connection.hashCode()))) {
      logger.trace("Accepted connection");

      this.connectionCount.incrementAndGet();

      try {
        connection.setSoTimeout(this.config.readTimeout);
      } catch (IOException e) {
        logger.warn("Exception while configuring client connection", e);
      }

      try (InputStream is = connection.getInputStream();
          OutputStream os = connection.getOutputStream()) {
        handler.handle(is, os);
      } catch(Throwable t) {
        logger.warn("Connection handler exception", t);
      }

      try {
        connection.close();
        logger.trace("Closed connection");
      } catch (IOException e) {
        logger.error("Connection to "
                     + connection.getRemoteSocketAddress()
                     + " encountered exception while closing",
                     e);
      }

      this.connectionCount.decrementAndGet();
    }
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
        logger.warn("Failed to wait for accept thread to join.");
      }
    } else {
      logger.info("Server socket was absent, missing call to start.");
    }
  }
}
