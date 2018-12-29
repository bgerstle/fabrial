package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.ClientSocketWrapper;
import com.eighthlight.fabrial.TcpServerSocket;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.Spliterators;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpServerSocketIntegrationTests {
  @Test
  void whenClientConnects_thenEmitsConnection() throws Exception {
    var serverSocket = new TcpServerSocket();
    var address = new InetSocketAddress(80);

    var futureAcceptedConnection = Executors.newSingleThreadExecutor().submit(() -> {
      return Spliterators.iterator(serverSocket.acceptConnections(address)).next();
    });

    try (var client = new TcpClient("localhost", 80)) {
      client.connect();
    }

    var acceptedConnection = futureAcceptedConnection.get(2, TimeUnit.SECONDS);

    assertTrue(acceptedConnection instanceof ClientSocketWrapper);
  }
}
