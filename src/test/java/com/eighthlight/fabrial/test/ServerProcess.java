package com.eighthlight.fabrial.test;

import org.apache.commons.io.input.TeeInputStream;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

public class ServerProcess {
  private static final Logger logger = Logger.getLogger(ServerProcess.class.getName());

  final Process app;
  BufferedReader bufferedStdOut;

  public ServerProcess() throws IOException {
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

  private String readStdErr(Duration timeout) throws IOException, InterruptedException {
    var stop = Instant.now().plus(timeout);
    var accumulatingBuffer = new ByteArrayOutputStream();
    var errStream = app.getErrorStream();
    while (Instant.now().isBefore(stop)) {
      var available = errStream.available();
      if (available < 1) {
        Thread.sleep(50);
        continue;
      }
      stop = Instant.now().plus(timeout);
      var readBuffer = new byte[available];
      var readCount = errStream.read(readBuffer);
      if (readCount > 0) {
        accumulatingBuffer.write(readBuffer, 0, readCount);
      }

    }
    return new String(accumulatingBuffer.toByteArray());
  }

  public void assertNoErrors() throws Exception {
    try {
      var appErrors = readStdErr(Duration.ofSeconds(1));
      if (appErrors.length() > 0) {
        logger.severe("App encountered errors:\n" + appErrors);
        fail();
      }
    } catch (IOException|InterruptedException e) {}
  }

  public void stop() {
    app.destroy();
  }
}
