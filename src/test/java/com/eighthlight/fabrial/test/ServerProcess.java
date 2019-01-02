package com.eighthlight.fabrial.test;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ServerProcess {
  Process serverProcess;

  public static ServerProcess start() throws IOException {
    return new ServerProcess();
  }

  private ServerProcess() throws IOException {
    serverProcess = new ProcessBuilder()
        .command("java", "-jar", "build/libs/fabrial-1.0-SNAPSHOT.jar")
        .inheritIO()
        .start();
  }

  public void stop() {
    serverProcess.destroy();
  }

  public int awaitTermination(int timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return serverProcess.onExit().get(timeout, unit).exitValue();
  }
}
