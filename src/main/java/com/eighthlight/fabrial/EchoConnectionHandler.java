package com.eighthlight.fabrial;

import java.io.IOException;
import java.util.logging.Logger;

public class EchoConnectionHandler implements ClientConnectionHandler {
  private static final Logger logger = Logger.getLogger(EchoConnectionHandler.class.getName());

  @Override
  public void handle(ClientConnection connection) {
    try (connection) {
      connection.getInputStream().transferTo(connection.getOutputStream());
    } catch (IOException e) {
      logger.warning("Connection error: " + e.getMessage());
    }
  }
}
