package com.eighthlight.fabrial.test.args;

import com.eighthlight.fabrial.server.ServerConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.eighthlight.fabrial.App.parseConfig;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ArgParserDirectoryIntegrationTest {

  @ParameterizedTest
  @ValueSource(strings = {"/", "/var", "/foo/bar/baz"})
  void parsesValidDirectories(String dirString) {
    Path dir = Paths.get(dirString);

    String[] args = List.of("-d", dirString).toArray(new String[0]);
    ServerConfig config = parseConfig(args).get();
    assertThat(config.port, equalTo(ServerConfig.DEFAULT_PORT));
    assertThat(config.directoryPath, equalTo(dir));
  }

  @Test
  void usesDefaultDirWhenUnspecified() {
    String[] args = List.of().toArray(new String[0]);
    ServerConfig config = parseConfig(args).get();
    assertThat(config.directoryPath, equalTo(ServerConfig.DEFAULT_DIRECTORY_PATH));
  }

  @Test
  void expandsRelativePaths() {
    String[] args = List.of("-d", "foo").toArray(new String[0]);
    ServerConfig config = parseConfig(args).get();
    assertThat(config.directoryPath, equalTo(Paths.get("foo").toAbsolutePath()));
  }
}
