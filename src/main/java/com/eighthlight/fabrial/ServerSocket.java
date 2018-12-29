package com.eighthlight.fabrial;

import java.util.stream.Stream;

public interface ServerSocket {
  Stream<ClientConnection> acceptConnections(int port);
}
