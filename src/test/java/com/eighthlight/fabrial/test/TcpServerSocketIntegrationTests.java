package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.TcpServerSocket;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpServerSocketIntegrationTests {
  @Test
  void whenClientConnects_thenEmitsConnection() throws Exception {
    var serverSocket = new TcpServerSocket();

    var futureAcceptedConnection = Executors.newSingleThreadExecutor().submit(() -> {
      return serverSocket.acceptConnections(80).findFirst();
    });

    try (var client = new TcpClient("localhost", 80)) {
      client.connect();
    }

    var acceptedConnection = futureAcceptedConnection.get(2, TimeUnit.SECONDS);

    assertTrue(acceptedConnection.isPresent());
  }
}
