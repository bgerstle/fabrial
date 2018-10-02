package com.eighthlight.fabrial.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IOStreamSupplier {
  public InputStream getInputStream() throws IOException;
  public OutputStream getOutputStream() throws IOException;
}
