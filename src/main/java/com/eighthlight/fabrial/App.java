package com.eighthlight.fabrial;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class App {
  public static final int DEFAULT_PORT = 80;

  public static void main(String[] args) throws Exception {
    System.out.println("Starting server...");
    try (var server = new TcpServer(new TcpServerSocket(), new ClientConnectionHandler() {
      @Override
      public void handle(ClientConnection connection) {

      }
    })) {
      server.start(80);
    }
//    try (var serverSocket = new ServerSocket()) {
//      serverSocket.bind(new InetSocketAddress(DEFAULT_PORT));
//      while (true) {
//        try (var connection = serverSocket.accept()) {
//          connection.getInputStream().transferTo(connection.getOutputStream());
//        }
//      }
//    }
  }
}
