package com.eighthlight.fabrial.test.fixtures;

import com.bgerstle.result.Result;
import com.eighthlight.fabrial.test.client.TcpClient;

import java.net.InetSocketAddress;

public class TcpClientFixture implements AutoCloseable {
  public final TcpClient client;

  public TcpClientFixture(int port) {
    client = new TcpClient(new InetSocketAddress(port));
  }

  @Override
  public void close() {
    if (!client.isClosed()) {
      Result.attempt(client::close).orElseAssert();
    }
  }
}
