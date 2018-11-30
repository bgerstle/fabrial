package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.server.AsyncServerSocketController;
import com.eighthlight.fabrial.server.ClientConnection;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class AsyncServerSocketControllerIntegrationTest {
  @Test
  void canConnectToBoundPort() throws IOException {
    var port = 8080;
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(ServerConfig.DEFAULT_READ_TIMEOUT)) {
      controller.start(port, 1, c -> {});
      clientFixture.client.connect(250, 3, 100);
    }
  }

  @Test
  void passesStreamsToConnectionHandler() throws IOException {
    var port = 8080;
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(ServerConfig.DEFAULT_READ_TIMEOUT)) {
      controller.start(port, 1, conn -> {
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
  void closesConnectionAfterHandling() throws IOException {
    var port = 8080;
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(ServerConfig.DEFAULT_READ_TIMEOUT)) {

      var connSpy = new AtomicReference<ClientConnection>(null);

      controller.start(port, 1, conn -> {
        connSpy.set(conn);
      });

      clientFixture.client.connect(250, 3, 100);

      assertThat(connSpy::get, eventuallyEval(notNullValue()));
      assertThat(() -> connSpy.get().isClosed(), eventuallyEval(equalTo(true)));
    }
  }

  @Test
  void setsReadTimeoutToPreventHangingConnections() throws Exception {
    var port = 8080;
    var futureReadTimeout = new CompletableFuture<IOException>();
    try (var clientFixture = new TcpClientFixture(port);
        var controller = new AsyncServerSocketController(100)) {
      controller.start(port, 1, conn -> {
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
      final var results =
          Collections.synchronizedList(new ArrayList<>(2));

      controller.start(port, 1, (conn) -> {
        try {
          var bufferedReader =
              new BufferedReader(new InputStreamReader(conn.getInputStream()));
          var input = bufferedReader.readLine();
          if (input.equals("crash")) {
            throw new RuntimeException(input);
          } else {
            var writer =
                new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write(input);
            writer.newLine();
            writer.flush();
          }
        } catch (IOException e) {
          throw new AssertionError(e);
        }
      });

      try (TcpClient crashClient = new TcpClient(new InetSocketAddress(port))) {
        crashClient.connect(250, 3, 100);
        var os = crashClient.getOutputStream();
        os.write("crash\n".getBytes());
        os.flush();
      }

      try (TcpClient okClient = new TcpClient(new InetSocketAddress(port))) {
        okClient.connect(250, 3, 100);
        var writer =
            new BufferedWriter(new OutputStreamWriter(okClient.getOutputStream()));
        var request = "foo";
        writer.write(request);
        writer.newLine();
        writer.flush();

        var reader =
            new BufferedReader(new InputStreamReader(okClient.getInputStream()));
        var response = reader.readLine();

        assertThat(response, equalTo(request));
      }
    }
  }
}
