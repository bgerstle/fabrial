package com.eighthlight.fabrial.test.http.server;

import com.eighthlight.fabrial.server.ClientConnection;
import com.eighthlight.fabrial.server.SocketController;

import java.io.IOException;
import java.util.function.Consumer;

class MockSocketController
    extends MockCloseable
    implements SocketController {
  public int boundPort = -1;
  public Consumer<ClientConnection> consumer;

  @Override
  public void start(int port, Consumer<ClientConnection> consumer) throws IOException {
    this.boundPort = port;
    this.consumer = consumer;
  }
}
