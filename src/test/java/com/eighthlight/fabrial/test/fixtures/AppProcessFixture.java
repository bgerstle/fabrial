package com.eighthlight.fabrial.test.fixtures;

import com.eighthlight.fabrial.server.ServerConfig;
import com.bgerstle.result.Result;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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
    var result = Result.attempt(() -> appProcess.waitFor(10, TimeUnit.SECONDS));

    // re-interrupt if interrupted
    result.getError().ifPresent(inte -> Thread.currentThread().interrupt());

    // capture exit value from first "destroy" attempt (or prior crash)
    var exited = result.getValue().get();
    var exitValue = appProcess.exitValue();

    // if initial destroy didn't work, try harder
    if (!exited) {
      appProcess.destroyForcibly();
    }

    // fail the test if the app didn't exit cleanly
    assertThat(exited, is(true));
    assertThat(exitValue, is(143));
  }
}
