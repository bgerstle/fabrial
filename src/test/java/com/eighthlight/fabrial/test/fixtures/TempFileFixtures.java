package com.eighthlight.fabrial.test.fixtures;

import com.eighthlight.fabrial.utils.Result;

import java.nio.file.Path;

public class TempFileFixtures {
  public static TempDirectoryFixture populatedTempDir() {
    return populatedTempDir(5, 3);
  }
  public static TempDirectoryFixture populatedTempDir(int numFiles, int depth) {
    // create base directory
    TempDirectoryFixture baseDirectoryFixture = new TempDirectoryFixture();
    // recursively populate with files/dirs
    createFileOrDirWithDepth(baseDirectoryFixture.tempDirPath, depth, numFiles);
    return baseDirectoryFixture;
  }

  private static void createFileOrDirWithDepth(Path currentDir, int depth, int numFiles) {
    if (depth == 0) {
      // reached max depth, fill with files
      for (int i = 0; i < numFiles; i++) {
        Result.attempt(() -> new TempFileFixture(currentDir)).orElseAssert();
      }
    } else {
      // create another depth level, then continue recursing
      TempDirectoryFixture nestedTempDir =
          Result.attempt(() -> new TempDirectoryFixture(currentDir))
                .orElseAssert();
      createFileOrDirWithDepth(nestedTempDir.tempDirPath, depth - 1, numFiles);
    }

  }
}
