package com.eighthlight.fabrial.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientSocketConnection implements ClientConnection {
  private final Socket socket;

  public ClientSocketConnection(Socket socket) {
    this.socket = socket;
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  @Override
  public boolean isClosed() {
    return socket.isClosed();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }
}
