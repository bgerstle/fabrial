package com.eighthlight.fabrial.server;

import java.io.IOException;

public interface ClientConnection extends IOStreamSupplier,
                                          AutoCloseable,
                                          CloseStateSupplier {
  @Override
  void close() throws IOException;
}
