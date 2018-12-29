package com.eighthlight.fabrial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ClientConnection {
  InputStream getInputStream() throws IOException;

  OutputStream getOutputStream() throws IOException;
}
