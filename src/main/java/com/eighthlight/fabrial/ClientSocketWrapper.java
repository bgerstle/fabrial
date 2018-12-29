package com.eighthlight.fabrial;

import java.net.Socket;

public class ClientSocketWrapper implements ClientConnection {
  private final Socket clientSocket;

  public ClientSocketWrapper(Socket clientSocket) {
    this.clientSocket = clientSocket;
  }
}
