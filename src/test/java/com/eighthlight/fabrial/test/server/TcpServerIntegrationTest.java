package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import com.eighthlight.fabrial.test.client.TcpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.fail;

public class TcpServerIntegrationTest {
  TcpClient client;
  TcpServer server;

  public TcpServer createServer() {
    return new TcpServer(new ServerConfig(8080, 50, ServerConfig.DEFAULT_DIRECTORY_PATH));
  }

  @BeforeEach
  void setUp() {
    // shorten read timeout for testing connection closures due to the socket being idle
    server = createServer();
    client = new TcpClient(new InetSocketAddress(server.config.port));
  }

  @AfterEach
  void tearDown() {
    try {
      if (!client.isClosed()) {
        client.close();
      }
      if (!server.isClosed()) {
        server.close();
      }
    } catch (IOException e) {
      fail(e);
    }
  }
}
