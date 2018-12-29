package com.eighthlight.fabrial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientSocketWrapper implements ClientConnection {
  private final Socket clientSocket;

  public ClientSocketWrapper(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return clientSocket.getOutputStream();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return clientSocket.getInputStream();
  }
}
