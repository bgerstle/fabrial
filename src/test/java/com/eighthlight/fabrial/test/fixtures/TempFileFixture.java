package com.eighthlight.fabrial.test.fixtures;

import com.bgerstle.result.Result;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class TempFileFixture implements AutoCloseable {
  public final Path tempFilePath;

  public TempFileFixture() {
    this(Paths.get("/tmp"), null);
  }

  public TempFileFixture(Path dir) {
    this(dir, null);
  }

  public TempFileFixture(Path dir, String suffix) {
    tempFilePath =
        Result.attempt(() -> Files.createTempFile(dir, "test", suffix))
              .orElseAssert();
  }

  public void write(InputStream is) {
    Result
        .attempt(() -> {
          assert tempFilePath.toFile().setWritable(true);
          try (var fw = new FileWriter(tempFilePath.toFile());
               var isr = new InputStreamReader(is)) {
            isr.transferTo(fw);
          }
        })
        .orElseAssert();
  }

  @Override
  public void close() {
      Result.attempt(() -> Files.deleteIfExists(tempFilePath));
  }
}
