package com.eighthlight.fabrial;

public class App {
  public static final int DEFAULT_PORT = 80;

  public static void main(String[] args) throws Exception {
    System.out.println("Starting server...");
    try (var server = new TcpServer(new ServerSocketConnection(), new EchoConnectionHandler())) {
      server.start(80);
    }
  }
}
