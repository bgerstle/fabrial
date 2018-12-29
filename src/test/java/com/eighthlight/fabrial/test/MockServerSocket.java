package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.ServerSocket;

import java.util.List;
import java.util.stream.Stream;

public class MockServerSocket implements ServerSocket {
  public final List<ClientConnection> connections;
  public int port;

  public MockServerSocket(List<ClientConnection> connections) {
    this.connections = connections;
    port = -1;
  }

  @Override
  public Stream<ClientConnection> acceptConnections(int port) {
    this.port = port;
    return connections.stream();
  }
}
