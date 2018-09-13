package com.eighthlight.fabrial.test.file;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.eighthlight.fabrial.utils.Result.attempt;

public class TempFileFixture implements AutoCloseable {
  public final Path tempFilePath;

  public TempFileFixture() {
    this(Paths.get("/tmp"));
  }

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
