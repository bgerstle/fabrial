package com.eighthlight.fabrial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Spliterator;

public interface ServerSocket extends AutoCloseable {
  Spliterator<ClientConnection> acceptConnections(InetSocketAddress address) throws IOException;
}
