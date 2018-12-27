package com.eighthlight.fabrial.test;

import junit.framework.AssertionFailedError;
import org.apache.commons.io.input.TeeInputStream;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

public class AppFixture {
  private static final Logger logger = Logger.getLogger(AppFixture.class.getName());

  final Process app;
  BufferedReader bufferedStdOut;

  public AppFixture() throws IOException {
    this.app =
        new ProcessBuilder(
            "java",
            "-jar",
            Paths.get(FileSystems.getDefault()
                                 .getPath("")
                                 .toAbsolutePath()
                                 .toString(),
                       "build",
                      "libs",
                      "fabrial-1.0-SNAPSHOT.jar")
                 .toString())
            .start();

    // "tee" app's stdout to this process, read from that output into a buffer for testing
    var teedStdout = new TeeInputStream(app.getInputStream(), System.out);
    bufferedStdOut = new BufferedReader(new InputStreamReader(teedStdout));
  }


  public String readOutputLine() throws IOException {
    return bufferedStdOut.readLine();
  }

  public void assertNoErrors() {
    try {
      var appErrors = new String(app.getErrorStream().readAllBytes());
      if (appErrors.length() > 0) {
        logger.severe("App encountered errors:\n" + appErrors);
        fail();
      }
    } catch (IOException e) {}
  }

  public void stop() {
    app.destroy();
  }
}
