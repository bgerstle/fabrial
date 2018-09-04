package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.test.client.TcpClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TcpServerConnectionIntegrationTest extends TcpServerIntegrationTest {
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
    TcpClient secondClient = new TcpClient(new InetSocketAddress(server.config.port));
    secondClient.connect();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(2)));
    client.close();
    secondClient.close();
    assertThat(() -> server.getConnectionCount(), eventuallyEval(is(0)));
  }
}
