package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.AccessLogResponder;
import com.eighthlight.fabrial.http.AccessLogger;
import com.eighthlight.fabrial.http.HttpConnectionHandler;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.file.LocalFilesystemController;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpServer implements Closeable {
  private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

  public final ServerConfig config;

  private final ConnectionHandler handler;

  private final SocketController socketController;

  public TcpServer(ServerConfig config) {
    this(config, new AccessLogger());
  }

  public TcpServer(ServerConfig config, AccessLogger accessLogger) {
    this(config,
         new HttpConnectionHandler(
             List.of(
                 new AccessLogResponder(accessLogger, config.adminCredential),
                 new FileHttpResponder(new LocalFilesystemController(config.directoryPath))),
             accessLogger::log),
         new AsyncServerSocketController(config.readTimeout));
  }

  public TcpServer(ServerConfig config, ConnectionHandler handler, SocketController socketController) {
    this.config = Objects.requireNonNull(config);
    this.handler = Objects.requireNonNull(handler);
    this.socketController = Objects.requireNonNull(socketController);
  }

  public int getPort() {
    return this.socketController.getPort();
  }

  public int getConnectionCount() {
    return socketController.getConnectionCount();
  }

  public int getPeakConnectionCount() {
    return socketController.getPeakConnectionCount();
  }

  /**
   * Start listening on the port in `config`.
   *
   * Must not be called more than once.
   *
   * @throws IOException If the underlying socket couldn't bind successfully.
   */
  public void start() throws IOException {
    socketController.start(config.port, this.config.maxConnections, this::handleConnection);
    logger.info("Server started on " + socketController.getPort());
    logger.info("Serving files from " + config.directoryPath);
  }

  // Handle new client connections
  private void handleConnection(ClientConnection connection) {
    var connectionId = Integer.toHexString(connection.hashCode());
    try (MDC.MDCCloseable cra = MDC.putCloseable("connectionId", connectionId)) {
      try (InputStream is = connection.getInputStream();
          OutputStream os = connection.getOutputStream()) {
        handler.handleConnectionStreams(is, os);
      } catch(Throwable t) {
        logger.warn("Connection handler exception", t);
      }
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
