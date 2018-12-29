package com.eighthlight.fabrial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Spliterator;

public interface ServerSocket {
  Spliterator<ClientConnection> acceptConnections(InetSocketAddress address) throws IOException;
}
