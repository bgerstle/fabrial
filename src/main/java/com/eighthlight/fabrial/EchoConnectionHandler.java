package com.eighthlight.fabrial;

import java.io.IOException;

public class EchoConnectionHandler implements ClientConnectionHandler {
  @Override
  public void handle(ClientConnection connection) {
    try {
      connection.getInputStream().transferTo(connection.getOutputStream());
      connection.close();
    } catch (IOException e) {

    }
  }
}
