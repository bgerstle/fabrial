package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import com.eighthlight.fabrial.test.fixtures.TcpServerFixture;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;

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

      assertThat(serverFixture.server.getPeakConnectionCount(), is(1));

    }
  }

  @Tag("slow")
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
                                Duration.ofSeconds(30)));
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

      assertThat(serverFixture.server.getPeakConnectionCount(), is(2));
    }
  }

  @Test
  void peakConnectionCountEqualsNumberOfSimultaneousClients() throws IOException {
    qt().withExamples(10).withShrinkCycles(0)
        .forAll(integers().between(1, 50)).checkAssert((clientCount) -> {
      Result.attempt(() -> {
        try (TcpServerFixture serverFixture =
            new TcpServerFixture(new ServerConfig(0,
                                                  ServerConfig.DEFAULT_READ_TIMEOUT,
                                                  ServerConfig.DEFAULT_DIRECTORY_PATH,
                                                  Optional.empty(),
                                                  clientCount))) {
          serverFixture.server.start();

          var clients = IntStream
              .iterate(clientCount, i -> i > 0, i -> i - 1)
              .parallel()
              .mapToObj(i -> {
                var client = new TcpClient(new InetSocketAddress(serverFixture.server.getPort()));
                Result.attempt(() -> {
                  client.connect();
                }).orElseAssert();
                return client;
              })
              .collect(Collectors.toList());

          try {
            assertThat(serverFixture.server::getPeakConnectionCount, eventuallyEval(is(clientCount)));
          } finally {
            clients.forEach(c -> Result.attempt(c::close).orElseAssert());
          }
        }
      }).orElseAssert();
    });
  }
}
