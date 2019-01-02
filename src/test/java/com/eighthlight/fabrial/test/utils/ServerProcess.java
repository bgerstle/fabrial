package com.eighthlight.fabrial.test.utils;

import org.apache.commons.io.input.TeeInputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.fail;

public class ServerProcess {
  private static final Logger logger = Logger.getLogger(ServerProcess.class.getName());

  private final Process app;

  private final BufferedReader bufferedStdOut;
  private final BufferedReader bufferedStdErr;

  private ExecutorService consoleLoggerService;

  public static ServerProcess start() throws IOException {
    return new ServerProcess();
  }

  private ServerProcess() throws IOException {
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

    // use `TeeInputStream` to stream server logs to the test console and simultaneously
    // feed them to buffered readers for use in tests

    var branchStdout = new PipedOutputStream();
    var teedStdout = new TeeInputStream(app.getInputStream(), branchStdout);
    bufferedStdOut = new BufferedReader(new InputStreamReader(new PipedInputStream(branchStdout)));

    var branchStderr = new PipedOutputStream();
    var teedStderr = new TeeInputStream(app.getErrorStream(), branchStderr);
    bufferedStdErr = new BufferedReader(new InputStreamReader(new PipedInputStream(branchStderr)));

    streamAppOutputToConsole(teedStdout, teedStderr);
  }

  private void streamAppOutputToConsole(InputStream stdout, InputStream stderr) {
    consoleLoggerService = Executors.newFixedThreadPool(2);
    logLinesWithPrefix("Server STDOUT: ", stdout, System.out);
    logLinesWithPrefix("Server STDERR: ", stderr, System.err);
  }

  private void logLinesWithPrefix(String prefix, InputStream src, PrintStream dest) {
    consoleLoggerService.submit(() -> {
      var reader = new BufferedReader(new InputStreamReader(src));
      reader.lines().forEach(l -> {
        dest.println(prefix + l);
      });
    });
  }

  public Boolean isAlive() {
    return app.isAlive();
  }

  public String readOutputLine() throws IOException {
    return bufferedStdOut.readLine();
  }

  private String readStdErr(Duration timeout) throws IOException, InterruptedException {
    var stop = Instant.now().plus(timeout);
    var accumulatingBuffer = new ByteArrayOutputStream();
    while (Instant.now().isBefore(stop)) {
      if (!bufferedStdErr.ready()) {
        Thread.sleep(50);
        continue;
      }
      stop = Instant.now().plus(timeout);
      var readBuffer = new char[128];
      var readCount = bufferedStdErr.read(readBuffer, 0, readBuffer.length);
      if (readCount > 0) {
        var readBytes = StandardCharsets.ISO_8859_1.encode(CharBuffer.wrap(readBuffer, 0, readCount));
        accumulatingBuffer.writeBytes(readBytes.array());
      }
    }
    return StandardCharsets.ISO_8859_1.decode(ByteBuffer.wrap(accumulatingBuffer.toByteArray())).toString();
  }

  public void assertNoErrors() {
    try {
      var appErrors = readStdErr(Duration.ofSeconds(1));
      if (appErrors.length() > 0) {
        logger.severe("App encountered errors:\n" + appErrors);
        fail();
      }
    } catch (IOException|InterruptedException e) {
      System.err.println("Encountered exception reading server app stderr: " + e.getMessage());
    }
  }

  public void stop() throws TimeoutException, ExecutionException, InterruptedException {
    app.destroy();
    app.onExit().get(5, TimeUnit.SECONDS);
    consoleLoggerService.shutdown();
    consoleLoggerService.awaitTermination(2, TimeUnit.SECONDS);
  }
}
