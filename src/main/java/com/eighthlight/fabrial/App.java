package com.eighthlight.fabrial;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class App {
  public static void main(String[] args) throws Exception {
    System.out.println("Starting server...");

    try (var serverSocket = new ServerSocket()) {

      serverSocket.bind(new InetSocketAddress(80));
      try (var connection = serverSocket.accept()) {
      }
    }
  }
}
