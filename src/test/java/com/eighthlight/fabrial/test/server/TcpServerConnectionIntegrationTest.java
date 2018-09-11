package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.http.TcpClientFixture;
import com.eighthlight.fabrial.test.http.TcpServerFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TcpServerConnectionIntegrationTest {
  @Test
  void connectionCountDecrementedByClientClosing() throws IOException {
    try (TcpServerFixture serverFixture =
        new TcpServerFixture(new ServerConfig(ServerConfig.DEFAULT_PORT,
                                              ServerConfig.DEFAULT_READ_TIMEOUT,
                                              ServerConfig.DEFAULT_DIRECTORY_PATH));
        TcpClientFixture clientFixture =
            new TcpClientFixture(serverFixture.server.config.port)) {
      serverFixture.server.start();
      clientFixture.client.connect();
      assertThat(() -> serverFixture.server.getConnectionCount(),
                 eventuallyEval(is(1)));
      clientFixture.client.close();
      assertThat(() -> serverFixture.server.getConnectionCount(),
                 eventuallyEval(is(0)));
    }
  }

  @Test
  void connectionCountDecrementedByServerStopping() throws IOException {
    try (TcpServerFixture serverFixture =
        new TcpServerFixture(new ServerConfig(ServerConfig.DEFAULT_PORT,
                                              ServerConfig.DEFAULT_READ_TIMEOUT,
                                              ServerConfig.DEFAULT_DIRECTORY_PATH));
        TcpClientFixture clientFixture =
            new TcpClientFixture(serverFixture.server.config.port)) {
      serverFixture.server.start();
      clientFixture.client.connect();
      assertThat(() -> serverFixture.server.getConnectionCount(),
                 eventuallyEval(is(1)));
      serverFixture.server.close();
      assertThat(() -> serverFixture.server.getConnectionCount(),
                 eventuallyEval(is(0),
                                Duration.ofSeconds(10)));
    }
  }

  @Test
  void connectionCountMatchesNumberOfClients() throws IOException {
    try (TcpServerFixture serverFixture =
        new TcpServerFixture(new ServerConfig(ServerConfig.DEFAULT_PORT,
                                              ServerConfig.DEFAULT_READ_TIMEOUT,
                                              ServerConfig.DEFAULT_DIRECTORY_PATH));
        TcpClientFixture clientFixture =
            new TcpClientFixture(serverFixture.server.config.port);
        TcpClientFixture secondClientFixture =
            new TcpClientFixture(serverFixture.server.config.port)) {
      serverFixture.server.start();
      clientFixture.client.connect();
      assertThat(() -> serverFixture.server.getConnectionCount(), eventuallyEval(is(1)));

      secondClientFixture.client.connect();
      assertThat(() -> serverFixture.server.getConnectionCount(), eventuallyEval(is(2)));
      clientFixture.client.close();
      secondClientFixture.client.close();
      assertThat(() -> serverFixture.server.getConnectionCount(), eventuallyEval(is(0)));
    }
  }
}
