package com.eighthlight.fabrial.test.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;

public class TcpClient implements AutoCloseable {
  public final SocketAddress address;
  private Socket socket;

  public TcpClient(SocketAddress address) {
    this.address = address;
    socket = new Socket();
  }

  public void connect() throws IOException {
    connect(100);
  }

  public void connect(int timeout) throws IOException {
    assert !socket.isConnected();
    socket.connect(address, timeout);
  }

  public boolean isClosed() {
    return socket.isClosed();
  }

  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }

  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
