package com.eighthlight.fabrial.test.fixtures;

import com.eighthlight.fabrial.test.client.TcpClient;

import java.net.InetSocketAddress;

import static com.eighthlight.fabrial.utils.Result.attempt;

public class TcpClientFixture implements AutoCloseable {
  public final TcpClient client;

  public TcpClientFixture(int port) {
    client = new TcpClient(new InetSocketAddress(port));
  }

  @Override
  public void close() {
    if (!client.isClosed()) {
      attempt(client::close).orElseAssert();
    }
  }
}
