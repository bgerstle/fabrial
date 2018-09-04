package com.eighthlight.fabrial.test.args;

import com.eighthlight.fabrial.server.ServerConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.eighthlight.fabrial.App.parseConfig;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArgParserIntegrationTest {
  @Test
  void emptyArgsYieldsDefaultConfig() {
    String[] args = List.of().toArray(new String[0]);
    ServerConfig config = parseConfig(args);
    assertThat(config.port, equalTo(ServerConfig.DEFAULT_PORT));
  }

  @Test
  void createsConfigWithSpecifiedPort() {
    String[] args = List.of("-p", "9000").toArray(new String[0]);
    ServerConfig config = parseConfig(args);
    assertThat(config.port, equalTo(9000));
  }
}
