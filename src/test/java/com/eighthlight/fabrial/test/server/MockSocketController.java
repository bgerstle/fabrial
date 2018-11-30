package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ClientConnection;
import com.eighthlight.fabrial.server.SocketController;

import java.io.IOException;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class MockSocketController
    extends MockCloseable
    implements SocketController {
  public int boundPort = -1;
  public int maxConnections = -1;
  public Consumer<ClientConnection> consumer;
  public Throwable closeConnectionError = null;
  public int connectionCount = -1;
  public int peakConnectionCount = -1;

  @Override
  public int getPort() {
    return boundPort;
  }

  @Override
  public void start(int port, int maxConnections, Consumer<ClientConnection> consumer) throws IOException {
    this.boundPort = port;
    this.maxConnections = maxConnections;
    this.consumer = consumer;
  }

  public void invokeHandlerWith(ClientConnection connection) {
    this.consumer.accept(connection);
    assertThat(connection.isClosed(), equalTo(false));
    try {
      connection.close();
    } catch (IOException e) {
      closeConnectionError = e;
    }
  }

  @Override
  public int getConnectionCount() {
    return connectionCount;
  }

  @Override
  public int getPeakConnectionCount() {
    return peakConnectionCount;
  }
}
