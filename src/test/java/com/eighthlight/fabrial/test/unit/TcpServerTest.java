package com.eighthlight.fabrial.test.unit;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.ClientConnectionHandler;
import com.eighthlight.fabrial.TcpServer;
import com.eighthlight.fabrial.test.mocks.MockClientConnection;
import com.eighthlight.fabrial.test.mocks.MockClientConnectionHandler;
import com.eighthlight.fabrial.test.mocks.MockServerConnection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.eighthlight.fabrial.App.DEFAULT_PORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TcpServerTest {
  @Test
  void givenPort_whenStarted_thenStartsAcceptingConnections() throws Exception {
    var port = 80;
    List<ClientConnection> connections = List.of(new MockClientConnection(), new MockClientConnection());
    var mockServerSocket = new MockServerConnection(connections);
    var mockConnectionHandler = new MockClientConnectionHandler();
    var server = new TcpServer(mockServerSocket, mockConnectionHandler);

    server.start(port);

    assertEquals(port, mockServerSocket.address.getPort());
    assertEquals(connections, mockConnectionHandler.handledConnections);
    assertTrue(mockConnectionHandler.handledConnections.stream().allMatch(c -> ((MockClientConnection)c).isClosed));
  }

  @Test
  void whenHandlerThrows_thenServerStillClosesConnection() throws Exception {
    var connection = new MockClientConnection();
    List<ClientConnection> connections = List.of(connection);
    var mockServerSocket = new MockServerConnection(connections);
    var mockConnectionHandler = new ClientConnectionHandler() {
      @Override
      public void handle(ClientConnection connection) {
        throw new RuntimeException("oops");
      }
    };
    var server = new TcpServer(mockServerSocket, mockConnectionHandler);

    server.start(DEFAULT_PORT);

    assertEquals(DEFAULT_PORT, mockServerSocket.address.getPort());
    assertTrue(connection.isClosed);
  }

  @Test
  void whenClosed_thenClosesSocket() throws Exception {
    var mockServerSocket = new MockServerConnection(List.of());
    var mockConnectionHandler = new MockClientConnectionHandler();
    var server = new TcpServer(mockServerSocket, mockConnectionHandler);

    server.close();

    assertTrue(mockServerSocket.isClosed);
  }
}
