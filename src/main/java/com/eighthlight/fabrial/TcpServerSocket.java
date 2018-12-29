package com.eighthlight.fabrial;

import java.util.stream.Stream;

public class TcpServerSocket implements ServerSocket {
  @Override
  public Stream<ClientConnection> acceptConnections(int port) {
    return Stream.of();
  }
}
