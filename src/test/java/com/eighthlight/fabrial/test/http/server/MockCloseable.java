package com.eighthlight.fabrial.test.http.server;

import com.eighthlight.fabrial.server.CloseStateSupplier;

import java.io.Closeable;
import java.io.IOException;

class MockCloseable implements Closeable, CloseStateSupplier {
  public boolean isClosed = false;

  @Override
  public void close() throws IOException {
    if (isClosed) {
      throw new IOException("Sorry, we're closed.");
    }
    isClosed = true;
  }

  @Override
  public boolean isClosed() {
    return isClosed;
  }
}
