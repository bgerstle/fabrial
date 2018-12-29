package com.eighthlight.fabrial.test;

import com.eighthlight.fabrial.ClientConnection;
import com.eighthlight.fabrial.EchoConnectionHandler;
import com.eighthlight.fabrial.TcpServer;
import com.eighthlight.fabrial.TcpServerSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TcpServerEchoIntegrationTest {
  TcpServer server;
  Thread serverThread;

  @BeforeEach
  void setUp() {
    server = new TcpServer(new TcpServerSocket(), new EchoConnectionHandler());
  }

  @AfterEach
  void tearDown() throws Exception {
    server.close();
    serverThread.join(2000);
  }

  void startServer(int port) {
    serverThread = new Thread(() -> {
      try {
        server.start(port);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    serverThread.start();
  }

  @Test
  void consecutiveConnections() throws Exception {
    startServer(80);
    var echoInputs = List.of("foo", "bar", "baz");

    var echoResponses =
        echoInputs.stream()
                  .map(input -> {
                    try (var client = TcpClient.forLocalServer()) {
                      client.connect();
                      return client.echo(input);
                    } catch (Exception e) {
                      fail(e);
                    }
                    return null;
                  })
                  .collect(Collectors.toList());

    assertEquals(echoInputs, echoResponses);
  }
}
