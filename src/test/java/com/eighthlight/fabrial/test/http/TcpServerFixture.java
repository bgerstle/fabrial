package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import com.eighthlight.fabrial.utils.Result;

public class TcpServerFixture implements AutoCloseable {
  public final TcpServer server;

  public TcpServerFixture(ServerConfig config) {
    server = new TcpServer(config);
  }

  @Override
  public void close() {
    if (!server.isClosed()) {
      Result.attempt(server::close).orElseAssert();
    }
  }
}
