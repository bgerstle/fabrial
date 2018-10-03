package com.eighthlight.fabrial.test.http.server;

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
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    assertThat(mockConn.isClosed, is(true));
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
          assertThat(conn.isClosed, is(true));
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
          public void start(int port, Consumer<ClientConnection> handler) throws IOException {
            throw new IOException("test bind error");
          }
        });

    assertThrows(IOException.class, server::start);
  }

  @Test
  void catchesHandlerErrorAndClosesConnection() throws IOException {
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
    mockController.consumer.accept(conn);

    assertThat(conn.isClosed, is(true));
  }

  @Test
  void catchesConnectionCloseErrors() throws IOException {
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
      public void close() throws IOException {
        throw new IOException("test close connection error");
      }
    };

    mockController.consumer.accept(conn);

    assertThat(conn.out.toString(), is(conn.in));
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

    mockController.consumer.accept(conn);

    assertThat(conn.isClosed, is(true));
  }

  @Test
  void handlesOutputStream() throws IOException {
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

    mockController.consumer.accept(conn);

    assertThat(conn.isClosed, is(true));
  }
}
