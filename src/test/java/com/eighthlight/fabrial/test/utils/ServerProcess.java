package com.eighthlight.fabrial.test.utils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServerProcess {
  private final Process app;

  public static ServerProcess start() throws IOException {
    return new ServerProcess();
  }

  private ServerProcess() throws IOException {
    this.app =
        new ProcessBuilder("java",
                           "-jar",
                           "build/libs/fabrial-1.0-SNAPSHOT.jar")
            .inheritIO()
            .start();
  }

  public int stop() throws TimeoutException, ExecutionException, InterruptedException {
    app.destroy();
    return app.onExit().get(5, TimeUnit.SECONDS).exitValue();
  }
}
