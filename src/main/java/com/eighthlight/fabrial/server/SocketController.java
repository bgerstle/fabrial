package com.eighthlight.fabrial.server;

import java.io.IOException;
import java.util.function.Consumer;

public interface SocketController extends AutoCloseable, CloseStateSupplier {
  public void start(
      int bindPort,
      Consumer<ClientConnection> connectionHandler
  ) throws IOException;

  @Override
  public void close() throws IOException;
}
