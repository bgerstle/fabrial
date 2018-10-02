package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.HttpConnectionHandler;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.file.LocalFilesystemController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpServer implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

  public final ServerConfig config;

  // number of clients currently connected to the server socket
  private final AtomicInteger connectionCount;

  private final ConnectionHandler handler;

  private final SocketController socketController;

  public TcpServer(ServerConfig config) {
    this(config,
         new HttpConnectionHandler(
             Set.of(
                 new FileHttpResponder(
                     new LocalFilesystemController(config.directoryPath)
                 ))),
         new AsyncServerSocketController(config.readTimeout));
  }

  public TcpServer(ServerConfig config, ConnectionHandler handler, SocketController socketController) {
    this.config = Objects.requireNonNull(config);
    this.handler = Objects.requireNonNull(handler);
    this.connectionCount = new AtomicInteger(0);
    this.socketController = Objects.requireNonNull(socketController);
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
    socketController.bindServer(config.port);
    logger.info("Server started on " + config.port);
    logger.info("Serving files from " + config.directoryPath);
    socketController.forEachConnection(this::handleConnection);
  }



  // Handle new client connections
  private void handleConnection(ClientConnection connection) {
    var connectionId = Integer.toHexString(connection.hashCode());
    try (MDC.MDCCloseable cra = MDC.putCloseable("connectionId", connectionId)) {
      logger.trace("Accepted connection");

      this.connectionCount.incrementAndGet();

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
        logger.warn("Connection encountered exception while closing", e);
      }

      this.connectionCount.decrementAndGet();
    }
  }

  /**
   * @return Whether or not the underlying socket is closed.
   */
  public boolean isClosed() {
    return socketController.isClosed();
  }

  public void close() throws IOException {
    logger.info("Closing...");
    if (!socketController.isClosed()) {
      socketController.close();
    }
  }
}
