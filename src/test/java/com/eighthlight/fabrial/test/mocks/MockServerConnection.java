package com.eighthlight.fabrial.test.mocks;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.ServerConnection;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Spliterator;

public class MockServerConnection implements ServerConnection {
  public final List<ClientConnection> connections;
  public InetSocketAddress address;
  public boolean isClosed = false;

  public MockServerConnection(List<ClientConnection> connections) {
    this.connections = connections;
  }

  @Override
  public Spliterator<ClientConnection> acceptConnections(InetSocketAddress address) {
    this.address = address;
    return connections.stream().spliterator();
  }

  @Override
  public void close() throws Exception {
    isClosed = true;
  }
}
