package com.eighthlight.fabrial.test.file;

import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.eighthlight.fabrial.utils.Result.attempt;

public class TempDirectoryFixture implements AutoCloseable {
  public final Path tempDirPath;

  private static final Logger logger = LoggerFactory.getLogger(TempFileFixtures.class);

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
    attempt(() -> {
      FileUtils.deleteDirectory(tempDirPath.toFile());
    })
        .getError().ifPresent((e) -> {
      logger.warn("Failed to delete temp dir",
                  StructuredArguments.kv("tempDirFixturePath", tempDirPath),
                  e);
    });
  }
}
