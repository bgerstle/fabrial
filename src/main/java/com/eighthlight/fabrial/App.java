package com.eighthlight.fabrial;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class App {
  public static void main(String[] args) throws Exception {
    ServerSocket socket = new ServerSocket();
    socket.bind(new InetSocketAddress(80));

    Socket clientConnection = socket.accept();

    clientConnection.getInputStream().transferTo(clientConnection.getOutputStream());
  }
}
