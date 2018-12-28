package com.eighthlight.fabrial.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.*;
import java.util.Optional;

public class TcpClient implements Closeable {
  private final Socket socket;

  TcpClient(String host, int port, int timeout) throws IOException {
    socket = new Socket();
    var address = new InetSocketAddress(host, port);
    socket.connect(address, timeout);
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
