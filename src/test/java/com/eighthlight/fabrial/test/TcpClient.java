package com.eighthlight.fabrial.test;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static com.eighthlight.fabrial.App.DEFAULT_PORT;

public class TcpClient implements Closeable {
  private Socket socket;
  final InetSocketAddress address;

  TcpClient(String host, int port) {
    address = new InetSocketAddress(host, port);
  }

  public static TcpClient forLocalServer() {
    return new TcpClient("localhost", DEFAULT_PORT);
  }

  public void connect() throws IOException, InterruptedException {
    connect(1000, 1000, 3);
  }

  public void connect(int timeout, int delay, int retries) throws IOException, InterruptedException {
    assert !isConnected();

    for (int i = retries; i >= 0; i--) {
      try {
        socket = new Socket();
        socket.connect(address, timeout);
        break;
      } catch (IOException e) {
        if (retries == 0) {
          throw e;
        }
      }
      // wait before retrying
      try {
        Thread.sleep(delay);
      } catch (InterruptedException ie) {
        throw ie;
      }
      // exponential backoff
      delay *= 2;
    }
  }

  public Boolean isClosed() {
    return socket == null || socket.isClosed();
  }

  public Boolean isConnected() {
    return socket != null && socket.isConnected();
  }

  @Override
  public void close() throws IOException {
    if (socket != null && socket.isConnected()) {
      socket.close();
      socket = null;
    }
  }

  public String echo(String input) throws IOException {
    var inputBytes = input.getBytes();
    socket.getOutputStream().write(inputBytes);
    var responseBytes = new byte[inputBytes.length];
    var readCount = socket.getInputStream().read(responseBytes);
    if (readCount < 1) {
      return "";
    }
    return new String(responseBytes, 0, readCount);
  }
}
