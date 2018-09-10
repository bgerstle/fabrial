package com.eighthlight.fabrial.test.file;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.eighthlight.fabrial.utils.Result.attempt;

public class TempDirectoryFixture implements AutoCloseable {
  public final Path tempDirPath;

  public TempDirectoryFixture() {
    this(null);
  }

  public TempDirectoryFixture(Path dir) {
    tempDirPath =
        attempt(() -> {
          if (dir == null) {
            return Files.createTempDirectory("test");
          } else {
            return Files.createTempDirectory(dir, "test");
          }
        })
            .orElseAssert();
  }

  @Override
  public void close() {
    attempt(() -> Files.deleteIfExists(tempDirPath));
  }
}
