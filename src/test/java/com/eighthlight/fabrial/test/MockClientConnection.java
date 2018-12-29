package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.ClientConnection;

import java.io.*;

public class MockClientConnection implements ClientConnection {
  public ByteArrayInputStream inputStream;
  public ByteArrayOutputStream outputStream;
  boolean isClosed = false;

  @Override
  public InputStream getInputStream() throws IOException {
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return outputStream;
  }

  @Override
  public void close() throws IOException {
    isClosed = true;
  }
}
