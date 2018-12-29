package com.eighthlight.fabrial.test.mocks;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.ClientConnectionHandler;

import java.util.ArrayList;

public class MockClientConnectionHandler implements ClientConnectionHandler {
  final ArrayList<ClientConnection> handledConnections;

  public MockClientConnectionHandler() {
    this.handledConnections = new ArrayList<>();
  }

  @Override
  public void handle(ClientConnection connection) {
    handledConnections.add(connection);
  }
}
