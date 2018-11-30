package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ClientConnection;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;

public class TcpServerTest {

  @Test
  void closeWithoutStarting() throws IOException {
    var server = new TcpServer(
        new ServerConfig(ServerConfig.DEFAULT_PORT,
                         ServerConfig.DEFAULT_READ_TIMEOUT,
                         ServerConfig.DEFAULT_DIRECTORY_PATH)
        , new EchoConnectionHandler(),
        new MockSocketController());
    server.close();
  }

  @Test
  void closesAfterStarting() throws IOException {
    var mockController = new MockSocketController();
    var server = new TcpServer(
        new ServerConfig(ServerConfig.DEFAULT_PORT,
                         ServerConfig.DEFAULT_READ_TIMEOUT,
                         ServerConfig.DEFAULT_DIRECTORY_PATH)
        , new EchoConnectionHandler(),
        mockController);
    server.start();
    assertThat(server.isClosed(), is(false));
    assertThat(mockController.isClosed(), is(false));
    server.close();
    assertThat(server.isClosed(), is(true));
    assertThat(mockController.isClosed(), is(true));
  }

  @ParameterizedTest
  @ValueSource(ints = {8080, 9000, 80})
  void startBindsOnExpectedPort(int port) throws IOException {
    var mockController = new MockSocketController();
    var config = new ServerConfig(port,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var server = new TcpServer(
        config
        , new EchoConnectionHandler(),
        mockController);
    server.start();
    assertThat(mockController.isClosed(), is(false));
    assertThat(mockController.boundPort, is(config.port));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 8, 500})
  void startsWithExpectedMaxConnections(int maxConnections) throws IOException {
    var mockController = new MockSocketController();
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH,
                                  Optional.empty(),
                                  maxConnections);
    var server = new TcpServer(
        config
        , new EchoConnectionHandler(),
        mockController);
    server.start();
    assertThat(mockController.maxConnections, is(maxConnections));
  }

  @Test
  void handlesConnection() throws IOException {
    var mockController = new MockSocketController();
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var server = new TcpServer(
        config
        , new EchoConnectionHandler(),
        mockController);
    server.start();
    assertThat(mockController.consumer, not(nullValue()));
    var mockConn = new MockClientConnection("foo");
    mockController.consumer.accept(mockConn);
    assertThat(mockConn.out.toString(), is(mockConn.in));
  }

  @Test
  void handlesConsecutiveConnections() throws IOException {
    var mockController = new MockSocketController();
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var server = new TcpServer(
        config
        , new EchoConnectionHandler(),
        mockController);
    server.start();
    assertThat(mockController.consumer, not(nullValue()));
    List.of("foo", "bar", "baz")
        .stream()
        .map(MockClientConnection::new)
        .forEach(conn -> {
          mockController.consumer.accept(conn);
          assertThat(conn.out.toString(), is(conn.in));
        });
  }

  @Test
  void propagatesBindingError() {
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var server = new TcpServer(
        config
        , new EchoConnectionHandler(),
        new MockSocketController() {
          @Override
          public void start(int port, int maxConnections, Consumer<ClientConnection> handler) throws IOException {
            throw new IOException("test bind error");
          }
        });

    assertThrows(IOException.class, server::start);
  }

  @Test
  void catchesHandlerError() throws IOException {
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var mockController = new MockSocketController();
    var server = new TcpServer(
        config,
        (is, os) -> { throw new IOException("test connection handler error"); },
        mockController);

    server.start();

    var conn = new MockClientConnection("");

    mockController.invokeHandlerWith(conn);
  }

  @Test
  void handlesInputStreamErrors() throws IOException {
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var mockController = new MockSocketController();
    var server = new TcpServer(
        config,
        new EchoConnectionHandler(),
        mockController);

    server.start();

    var conn = new MockClientConnection("baz") {
      @Override
      public InputStream getInputStream() throws IOException {
        throw new IOException("test in stream error");
      }
    };

    mockController.invokeHandlerWith(conn);
  }

  @Test
  void handlesOutputStreamErrors() throws IOException {
    var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                  ServerConfig.DEFAULT_DIRECTORY_PATH);
    var mockController = new MockSocketController();
    var server = new TcpServer(
        config,
        new EchoConnectionHandler(),
        mockController);

    server.start();

    var conn = new MockClientConnection("baz") {
      @Override
      public OutputStream getOutputStream() throws IOException {
        throw new IOException("test out stream error");
      }
    };

    mockController.invokeHandlerWith(conn);
  }

  @Test
  void returnsSocketControllerConnectionCounts() {
    qt().withExamples(10)
        .forAll(integers().all(), integers().all())
        .checkAssert((connectionCount, peakConnectionCount) -> {
          var config = new ServerConfig(ServerConfig.DEFAULT_PORT,
                                        ServerConfig.DEFAULT_READ_TIMEOUT,
                                        ServerConfig.DEFAULT_DIRECTORY_PATH);
          var mockController = new MockSocketController();
          mockController.connectionCount = connectionCount;
          mockController.peakConnectionCount = peakConnectionCount;
          var server = new TcpServer(
              config,
              new EchoConnectionHandler(),
              mockController);

          assertThat(server.getConnectionCount(), equalTo(connectionCount));
          assertThat(server.getPeakConnectionCount(), equalTo(peakConnectionCount));
        });
  }
}
