package com.eighthlight.fabrial;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class App {
  public static final int DEFAULT_PORT = 80;

  public static void main(String[] args) throws Exception {
    System.out.println("Starting server...");
    try (var server = new TcpServer(new TcpServerSocket(), new EchoConnectionHandler())) {
      server.start(80);
    }
  }
}
