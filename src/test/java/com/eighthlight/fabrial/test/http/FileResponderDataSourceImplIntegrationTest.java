package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.file.FileResponderFileControllerImpl;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixture;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class FileResponderDataSourceImplIntegrationTest {
  @Test
  void respondsWithDirectoryContents() {
    try (var tmpDirFixture = new TempDirectoryFixture();
         var tmpFileFixture = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var dataSource = new FileResponderFileControllerImpl(tmpDirFixture.tempDirPath);

      assertThat(dataSource.fileExistsAtPath("/"), equalTo(true));

      assertThat(dataSource.isDirectory("/"), equalTo(true));

      assertThat(dataSource.getDirectoryContents("/"),
                 equalTo(List.of(tmpFileFixture.tempFilePath.getFileName().toString())));
    }
  }

  @Test
  void returnsNullWhenNotDirectory() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var dataSource =
          new FileResponderFileControllerImpl(tmpFileFixture.tempFilePath.getParent().toAbsolutePath());

      assertThat(dataSource.isDirectory(tmpFileFixture.tempFilePath.getFileName().toString()),
                 is(false));
      assertThat(dataSource.getDirectoryContents(tmpFileFixture.tempFilePath.getFileName().toString()),
                 is(nullValue()));
    }
  }

  @Test
  void returnsAllFilesInDirWithMultipleFiles() {
    try (var tmpDirFixture = new TempDirectoryFixture();
        var tmpFileFixture1 = new TempFileFixture(tmpDirFixture.tempDirPath);
        var tmpFileFixture2 = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var dataSource =
          new FileResponderFileControllerImpl(tmpDirFixture.tempDirPath);
      assertThat(dataSource.getDirectoryContents("/"),
                 containsInAnyOrder(tmpFileFixture1.tempFilePath.getFileName().toString(),
                                    tmpFileFixture2.tempFilePath.getFileName().toString()));
    }
  }

  @Test
  void returnsZeroForEmptyFile() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var dataSource =
          new FileResponderFileControllerImpl(tmpFileFixture.tempFilePath.getParent());

      var actualSize = dataSource.getFileSize(tmpFileFixture.tempFilePath.getFileName().toString());
      assertThat(actualSize, is(0L));
    }
  }

  @Test
  void returnsCorrectSizeAndDataForNonEmptyFile() throws IOException {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt")) {
      String tmpFilename = tmpFileFixture.tempFilePath.getFileName().toString();

      var dataSource =
          new FileResponderFileControllerImpl(tmpFileFixture.tempFilePath.getParent());

      var testData = "foo".getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(testData));

      assertThat(dataSource.getFileSize(tmpFilename),
                 is((long)testData.length));

      var contents = dataSource.getFileContents(tmpFilename);
      var readBytes =
          Optional.ofNullable(contents)
                  .map(is -> Result.attempt(is::readAllBytes).orElseAssert());
      assertThat(readBytes.orElse(null), is(testData));

      assertThat(dataSource.getFileMimeType(tmpFilename), startsWith("text/plain"));
    }
  }
}
