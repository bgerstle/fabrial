package com.eighthlight.fabrial.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpClient implements Closeable {
  final String host;
  final int port;
  Socket socket;

  public static TcpClient forLocalServer() {
    return new TcpClient("localhost", 80);
  }

  public TcpClient(String host, int port) {
    this.host = host;
    this.port = port;
  }

  public void connect(int retries) throws IOException, InterruptedException {
    socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(host, port), 1000);
    } catch (IOException e) {
      if (retries == 0) {
        throw e;
      }
      Thread.sleep(500);
      connect(retries - 1);
    }
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  public byte[] echo(byte[] data) throws IOException {
    socket.getOutputStream().write(data);
    var readBuffer = new byte[data.length];
    socket.getInputStream().read(readBuffer);
    return readBuffer;
  }
}
