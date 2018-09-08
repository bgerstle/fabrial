package com.eighthlight.fabrial.test.http;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.eighthlight.fabrial.utils.Result.attempt;

public class TempFileFixture implements AutoCloseable {
  public final Path tempFilePath;

  public TempFileFixture(Path dir) {
    tempFilePath =
        attempt(() -> Files.createTempFile(dir, "test", null))
              .orElseAssert();
  }

  @Override
  public void close() {
      attempt(() -> Files.deleteIfExists(tempFilePath));
  }
}
