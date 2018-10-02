package com.eighthlight.fabrial.server;

import java.io.IOException;
import java.util.function.Consumer;

public interface SocketController extends AutoCloseable, CloseStateSupplier {
  public void bindServer(int port) throws IOException;
  public void forEachConnection(Consumer<ClientConnection> consumer);

  @Override
  public void close() throws IOException;
}
