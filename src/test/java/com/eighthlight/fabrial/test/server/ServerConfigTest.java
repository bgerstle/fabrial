package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ServerConfigTest {
  @Test
  void customPort() {
    int port = 81;
    ServerConfig conf = new ServerConfig(port,
                                         ServerConfig.DEFAULT_READ_TIMEOUT,
                                         ServerConfig.DEFAULT_DIRECTORY_PATH);
    assertThat(conf.port, equalTo(port));
  }
}
