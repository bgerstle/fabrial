package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.server.ConnectionHandler;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TcpServerTest {
  static class NoopConnectionHandler implements ConnectionHandler {
    @Override
    public void handle(InputStream is, OutputStream os) throws Throwable {}
  }

  @Test
  void closeWithoutStarting() throws IOException {
    var server = new TcpServer(
        new ServerConfig(ServerConfig.DEFAULT_PORT,
                         ServerConfig.DEFAULT_READ_TIMEOUT,
                         ServerConfig.DEFAULT_DIRECTORY_PATH)
        , new NoopConnectionHandler());
    server.close();
  }
}
