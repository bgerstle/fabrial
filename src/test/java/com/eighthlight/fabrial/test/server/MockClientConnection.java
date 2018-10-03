package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ClientConnection;

import java.io.*;

class MockClientConnection
    extends MockCloseable
    implements ClientConnection {
  public final String in;
  public final ByteArrayOutputStream out;

  public MockClientConnection(String in) {
    this.in = in;
    this.out = new ByteArrayOutputStream();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return new ByteArrayInputStream(in.getBytes());
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return out;
  }
}
