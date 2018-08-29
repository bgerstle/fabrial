package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import com.eighthlight.fabrial.test.TcpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class TcpServerAcceptanceTest {
  TcpClient client;
  TcpServer server;

  @BeforeEach
  void setUp() {
    client = new TcpClient(new InetSocketAddress(8080));
    // shorten read timeout for testing connection closures due to the socket being idle
    server = new TcpServer(new ServerConfig(8080,
                                            ServerConfig.DEFAULT_MAX_CONNECTIONS,
                                            50));
  }

  @AfterEach
  void tearDown() {
    try {
      if (!client.isClosed()) {
        client.close();
      }
      if (!server.isClosed()) {
        server.close();
      }
    } catch (IOException e) {
      fail(e);
    }
  }

  @Test
  void connectionCountDecrementedByClientClosing() throws IOException {
    server.start();
    client.connect();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(1)));
    client.close();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(0)));
  }

  @Test
  void connectionCountDecrementedByServerStopping() throws IOException {
    server.start();
    client.connect();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(1)));
    server.close();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(0)));
  }

  @Test
  void connectionCountMatchesNumberOfClients() throws IOException {
    server.start();
    client.connect();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(1)));
    TcpClient secondClient = new TcpClient(new InetSocketAddress(8080));
    secondClient.connect();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(2)));
    client.close();
    secondClient.close();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(0)));
  }
}
