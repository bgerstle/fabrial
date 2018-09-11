package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.utils.Result;

import java.util.Optional;

public class AppProcessFixture implements AutoCloseable {
  public final Process appProcess;

  public AppProcessFixture(Integer port, String directory) {
    appProcess =
        Result.attempt(() ->
                           new ProcessBuilder(
                               "java",
                               // propagate log level to app process
                               "-DrootLevel="
                               + Optional.ofNullable(System.getProperty("rootLevel")).orElse("debug"),
                               // suppress logback errors
                               "-Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener",
                               "-jar",
                               // hard-coded to SNAPSHOT version. might need to fix this eventually...
                               "./build/libs/fabrial-all-1.0-SNAPSHOT.jar",
                               "-p", Optional.ofNullable(port)
                                             .orElse(ServerConfig.DEFAULT_PORT).toString(),
                               "-d", Optional.ofNullable(directory)
                                             .orElse(ServerConfig.DEFAULT_DIRECTORY_PATH.toString()))
                               .inheritIO()
                               .start())
              .orElseAssert();
  }

  @Override
  public void close() {
    appProcess.destroy();
  }
}
