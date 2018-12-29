package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.TcpServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpServerTest {
  @Test
  void givenPort_whenStarted_thenStartsAcceptingConnections() throws Exception {
    var port = 80;
    List<ClientConnection> connections = List.of(new MockClientConnection(), new MockClientConnection());
    var mockServerSocket = new MockServerSocket(connections);
    var mockConnectionHandler = new MockClientConnectionHandler();
    var server = new TcpServer(mockServerSocket, mockConnectionHandler);

    server.start(port);

    assertEquals(port, mockServerSocket.address.getPort());
    assertEquals(connections, mockConnectionHandler.handledConnections);
  }

  @Test
  void whenClosed_thenClosesSocket() throws Exception {
    var mockServerSocket = new MockServerSocket(List.of());
    var mockConnectionHandler = new MockClientConnectionHandler();
    var server = new TcpServer(mockServerSocket, mockConnectionHandler);
    server.close();
    assertTrue(mockServerSocket.isClosed);
  }
}
