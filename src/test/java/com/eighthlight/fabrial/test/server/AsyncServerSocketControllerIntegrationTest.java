package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.AsyncServerSocketController;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.http.TcpClientFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AsyncServerSocketControllerIntegrationTest {
  @Test
  void canConnectToBoundPort() throws IOException {
    var port = 8080;
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(ServerConfig.DEFAULT_READ_TIMEOUT)) {
      controller.bindServer(port);
      clientFixture.client.connect(250, 3, 100);
    }
  }

  @Test
  void passesStreamsToConnectionHandler() throws IOException {
    var port = 8080;
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(ServerConfig.DEFAULT_READ_TIMEOUT)) {
      controller.bindServer(port);
      controller.forEachConnection(conn -> {
        try {
          conn.getInputStream().transferTo(conn.getOutputStream());
        } catch (IOException e) {
          throw new AssertionError(e);
        }
      });

      clientFixture.client.connect(250, 3, 100);

      var writtenData = "foo".getBytes();
      clientFixture.client.getOutputStream().write(writtenData);

      var byteBuffer = ByteBuffer.allocate(writtenData.length);
      var readLen = clientFixture.client.getInputStream().read(byteBuffer.array());

      assertThat(readLen, equalTo(writtenData.length));
      assertThat(byteBuffer.array(), equalTo(writtenData));
    }
  }

  @Test
  void setsReadTimeoutToPreventHangingConnections() throws Exception {
    var port = 8080;
    var futureReadTimeout = new CompletableFuture<IOException>();
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(100)) {
      controller.bindServer(port);
      controller.forEachConnection(conn -> {
        try {
          conn.getInputStream().transferTo(conn.getOutputStream());
        } catch (IOException e) {
          futureReadTimeout.complete(e);
        }
      });

      clientFixture.client.connect(250, 3, 100);

      assertThat(futureReadTimeout::isDone,
                 eventuallyEval(equalTo(true), Duration.ofMillis(200)));

      assertThat(futureReadTimeout.get().getMessage(),
                 equalTo("Read timed out"));
    }
  }

  @Test
  void closesCompletelyWhenReturningFromClose() throws IOException {
    // start/close servers in a loop and guarantee that no "port in use"
    // errors occur
    var port = 8080;
    var futureReadTimeout = new CompletableFuture<IOException>();
    for (int i = 0; i < 5; i++) {
      var controller = new AsyncServerSocketController(0);
      controller.bindServer(port);
      controller.close();
    }
  }
}
