package com.eighthlight.fabrial.test.integration;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.ClientSocketConnection;
import com.eighthlight.fabrial.ServerSocketConnection;
import com.eighthlight.fabrial.test.utils.TcpClient;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Spliterators;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class ServerSocketConnectionIntegrationTests {
  @Test
  void whenClientConnects_thenEmitsConnection() throws Exception {
    var serverSocket = new ServerSocketConnection();
    var address = new InetSocketAddress(80);

    var futureAcceptedConnection = Executors.newSingleThreadExecutor().submit(() -> {
      return Spliterators.iterator(serverSocket.acceptConnections(address)).next();
    });

    try (var client = new TcpClient("localhost", 80)) {
      client.connect();
    }

    var acceptedConnection = futureAcceptedConnection.get(2, TimeUnit.SECONDS);

    assertTrue(acceptedConnection instanceof ClientSocketConnection);

    serverSocket.close();
  }

  @Test
  void consecutiveConnections() throws Exception {
    var serverSocket = new ServerSocketConnection();
    var address = new InetSocketAddress(80);
    var numConnections = 2;

    var futureAcceptedConnections = Executors.newSingleThreadExecutor().submit(() -> {
      var acceptedConnections  = new ArrayList<ClientConnection>();

      var connIterator = serverSocket.acceptConnections(address);

      for (int i = 0; i < numConnections; i++) {
        assertTrue(connIterator.tryAdvance(acceptedConnections::add));
      }

      return acceptedConnections;
    });

    for (int i = 0; i < numConnections; i++) {
      try (var client = new TcpClient("localhost", 80)) {
        client.connect();
      }
    }

    var acceptedConnections = futureAcceptedConnections.get(2, TimeUnit.SECONDS);
    assertEquals(numConnections, acceptedConnections.size());

    serverSocket.close();
  }

  @Test
  void whenSocketClosed_thenFailsToAdvance() throws Exception {
    var serverSocket = new ServerSocketConnection();
    var address = new InetSocketAddress(80);

    var iter = serverSocket.acceptConnections(address);
    serverSocket.close();
    var didAdvance = iter.tryAdvance(unused -> fail());

    assertFalse(didAdvance);
  }
}
