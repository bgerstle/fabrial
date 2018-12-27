package com.eighthlight.fabrial.test;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Optional;

public class AppFixture {
  final Process app;
  BufferedReader bufferedIn;

  public AppFixture() throws IOException {
    this.app =
        new ProcessBuilder(
            "java",
            "-jar",
            Paths.get(FileSystems.getDefault().getPath("").toAbsolutePath().toString(),
                       "build/libs/fabrial-1.0.0-SNAPSHOT.jar").toString())
            .inheritIO()
            .start();
  }

  public String readOutputLine() throws IOException {
    if (bufferedIn == null) {
      bufferedIn = new BufferedReader(new InputStreamReader(app.getInputStream()));
    }
    return bufferedIn.readLine();
  }

  public void stop() {
    app.destroy();
  }
}
