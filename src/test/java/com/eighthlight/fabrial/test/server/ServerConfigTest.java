package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ServerConfigTest {
  @Test
  void defaultPort() {
    ServerConfig conf = new ServerConfig();
    assertThat(conf.port, equalTo(ServerConfig.DEFAULT_PORT));
  }

  @Test
  void customPort() {
    int port = 81;
    ServerConfig conf = new ServerConfig(port);
    assertThat(conf.port, equalTo(port));
    assertThat(conf.maxConnections, equalTo(ServerConfig.DEFAULT_MAX_CONNECTIONS));
  }
}
