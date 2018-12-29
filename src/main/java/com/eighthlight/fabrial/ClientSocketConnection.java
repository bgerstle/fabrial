package com.eighthlight.fabrial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ClientSocketConnection implements ClientConnection {
  private final Socket clientSocket;

  public ClientSocketConnection(Socket clientSocket) {
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

  @Override
  public void close() throws IOException {
    clientSocket.close();
  }
}
