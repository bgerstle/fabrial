package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.AsyncServerSocketController;
import com.eighthlight.fabrial.server.ClientConnection;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.http.TcpClientFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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
      controller.forEachConnection(c -> {});
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
  void handlesConnectionAfterError() throws IOException {
    var port = 8080;
    try (var controller = new AsyncServerSocketController(ServerConfig.DEFAULT_READ_TIMEOUT)) {
      var results = new ArrayList<Object>(2);
      results.add(new IOException("error"));
      results.add("foo");

      controller.bindServer(port);
      controller.forEachConnection(new Consumer<ClientConnection>() {
        @Override
        public void accept(ClientConnection conn) {
          var result = results.get(0);
          results.remove(0);
          if (result instanceof Exception) {
            throw new RuntimeException((IOException)result);
          } else {
            try {
              conn.getOutputStream().write(((String) result).getBytes());
            } catch (IOException e) {
              throw new AssertionError(e);
            }
          }
        }
      });

      TcpClient client = null;
      for (int i = 0; i < results.size(); i++) {
        client = new TcpClient(new InetSocketAddress(port));;
        client.connect(100, 3, 100);
      }
      var byteBuffer = ByteBuffer.allocate(3);
      client.getInputStream().read(byteBuffer.array());
      assertThat(byteBuffer.array(), equalTo("foo".getBytes()));
    }
  }
}
