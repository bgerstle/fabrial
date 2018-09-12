package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.file.FileResponderDataSourceImpl;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixture;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileResponderDataSourceImplIntegrationTest {
  @Test
  void respondsWithDirectoryContents() {
    try (var tmpDirFixture = new TempDirectoryFixture();
         var tmpFileFixture = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var dataSource = new FileResponderDataSourceImpl(tmpDirFixture.tempDirPath);

      assertThat(dataSource.fileExistsAtPath(tmpDirFixture.tempDirPath), equalTo(true));

      assertThat(dataSource.isDirectory(tmpDirFixture.tempDirPath), equalTo(true));

      assertThat(dataSource.getDirectoryContents(tmpDirFixture.tempDirPath),
                 equalTo(tmpFileFixture.tempFilePath.getFileName()));
    }
  }
}
