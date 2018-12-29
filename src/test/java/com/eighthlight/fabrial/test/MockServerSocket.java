package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.ServerSocket;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Stream;

public class MockServerSocket implements ServerSocket {
  public final List<ClientConnection> connections;
  public InetSocketAddress address;
  public boolean isClosed = false;

  public MockServerSocket(List<ClientConnection> connections) {
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
