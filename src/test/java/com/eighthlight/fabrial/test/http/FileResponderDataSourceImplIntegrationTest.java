package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.file.FileResponderDataSourceImpl;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixture;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileResponderDataSourceImplIntegrationTest {
  @Test
  void respondsWithDirectoryContents() {
    try (var tmpDirFixture = new TempDirectoryFixture();
         var tmpFileFixture = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var dataSource = new FileResponderDataSourceImpl(tmpDirFixture.tempDirPath);

      assertThat(dataSource.fileExistsAtPath(Paths.get("/")), equalTo(true));

      assertThat(dataSource.isDirectory(Paths.get("/")), equalTo(true));

      assertThat(dataSource.getDirectoryContents(Paths.get("/")),
                 equalTo(List.of(tmpFileFixture.tempFilePath.getFileName())));
    }
  }

  @Test
  void returnsNullWhenNotDirectory() {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"))) {
      var dataSource =
          new FileResponderDataSourceImpl(tmpFileFixture.tempFilePath.getParent().toAbsolutePath());

      assertThat(dataSource.isDirectory(tmpFileFixture.tempFilePath.getFileName()),
                 is(false));
      assertThat(dataSource.getDirectoryContents(tmpFileFixture.tempFilePath.getFileName()),
                 is(nullValue()));
    }
  }
}
