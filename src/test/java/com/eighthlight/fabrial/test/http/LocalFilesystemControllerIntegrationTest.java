package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.file.LocalFilesystemController;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixture;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocalFilesystemControllerIntegrationTest {
  @Test
  void detectsExistingDirs() {
   try (var tmpDirFixture = new TempDirectoryFixture()) {
     var fileController = new LocalFilesystemController(tmpDirFixture.tempDirPath);

     assertThat(fileController.fileExistsAtPath("/"), is(true));
   }
  }

  @Test
  void detectsExistingEmptyFiles() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());

      var filename = tmpFileFixture.tempFilePath.getFileName().toString();
      assertThat(fileController.fileExistsAtPath(filename),  is(true));
    }
  }

  @Test
  void detectsExistingFilesWithData() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());

      tmpFileFixture.write(new ByteArrayInputStream("foo".getBytes()));

      var filename = tmpFileFixture.tempFilePath.getFileName().toString();
      assertThat(fileController.fileExistsAtPath("/" + filename),  is(true));
    }
  }

  @Test
  void detectsFilesThatWereJustCreated() throws IOException {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var fileController =
          new LocalFilesystemController(tmpDirFixture.tempDirPath);

      var newFileName = "foo";

      assertThat(fileController.fileExistsAtPath(newFileName), is(false));


      try (var fileWriter = new FileOutputStream(Paths.get(tmpDirFixture.tempDirPath.toString(), newFileName).toFile())) {
        fileWriter.write("foo".getBytes());
      }

      assertThat(fileController.fileExistsAtPath(newFileName),  is(true));
    }
  }

  @Test
  void detectsAbsentFiles() {
    var fileController =
        new LocalFilesystemController(Paths.get(UUID.randomUUID().toString()));
    assertThat(fileController.fileExistsAtPath("/"), is(false));
  }


  @Test
  void respondsWithDirectoryContents() {
    try (var tmpDirFixture = new TempDirectoryFixture();
         var tmpFileFixture = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var fileController = new LocalFilesystemController(tmpDirFixture.tempDirPath);

      assertThat(fileController.fileExistsAtPath("/"), equalTo(true));

      assertThat(fileController.isDirectory("/"), equalTo(true));

      assertThat(fileController.getDirectoryContents("/"),
                 equalTo(List.of(tmpFileFixture.tempFilePath.getFileName().toString())));
    }
  }

  @Test
  void returnsNullWhenNotDirectory() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent().toAbsolutePath());

      assertThat(fileController.isDirectory(tmpFileFixture.tempFilePath.getFileName().toString()),
                 is(false));
      assertThat(fileController.getDirectoryContents(tmpFileFixture.tempFilePath.getFileName().toString()),
                 is(nullValue()));
    }
  }

  @Test
  void returnsAllFilesInDirWithMultipleFiles() {
    try (var tmpDirFixture = new TempDirectoryFixture();
        var tmpFileFixture1 = new TempFileFixture(tmpDirFixture.tempDirPath);
        var tmpFileFixture2 = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var fileController =
          new LocalFilesystemController(tmpDirFixture.tempDirPath);
      assertThat(fileController.getDirectoryContents("/"),
                 containsInAnyOrder(tmpFileFixture1.tempFilePath.getFileName().toString(),
                                    tmpFileFixture2.tempFilePath.getFileName().toString()));
    }
  }

  @Test
  void returnsZeroForEmptyFile() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());

      var actualSize = fileController.getFileSize(tmpFileFixture.tempFilePath.getFileName().toString());
      assertThat(actualSize, is(0L));
    }
  }

  @Test
  void returnsCorrectSizeAndDataForNonEmptyFile() throws IOException {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt")) {
      String tmpFilename = tmpFileFixture.tempFilePath.getFileName().toString();

      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());

      var testData = "foo".getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(testData));

      assertThat(fileController.getFileSize(tmpFilename),
                 is((long)testData.length));

      var contents = fileController.getFileContents(tmpFilename);
      var readBytes =
          Optional.ofNullable(contents)
                  .map(is -> Result.attempt(is::readAllBytes).orElseAssert());
      assertThat(readBytes.orElse(null), is(testData));

      assertThat(fileController.getFileMimeType(tmpFilename), startsWith("text/plain"));
    }
  }
  
  @Test
  void updatesExistingFile() throws IOException {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt")) {
      var data = "foo".getBytes();
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());

      fileController.updateFileContents(tmpFileFixture.tempFilePath.getFileName().toString(),
                                        new ByteArrayInputStream(data),
                                        data.length);
      var tmpFile = tmpFileFixture.tempFilePath.toFile();
      assertThat(tmpFile.length(), is(Integer.toUnsignedLong(data.length)));
      var fileStream = new FileInputStream(tmpFile);
      assertThat(fileStream.readAllBytes(), is(data));
    }
  }

  @Test
  void createsNewFiles() throws IOException {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var data = "foo".getBytes();
      var fileController =
          new LocalFilesystemController(tmpDirFixture.tempDirPath);

      fileController.updateFileContents("foo",
                                        new ByteArrayInputStream(data),
                                        data.length);
      var newFilePath = Paths.get(tmpDirFixture.tempDirPath.toString(), "foo");
      var tmpFile = newFilePath.toFile();
      assertThat(tmpFile.length(), is(Integer.toUnsignedLong(data.length)));
      var fileStream = new FileInputStream(tmpFile);
      assertThat(fileStream.readAllBytes(), is(data));
    }
  }

  @Test
  void failsToUpdateExistingDirs() throws Exception {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var data = "foo".getBytes();
      var fileController =
          new LocalFilesystemController(tmpDirFixture.tempDirPath);
      assertThrows(FileNotFoundException.class, () -> {
        fileController.updateFileContents("/",
                                          new ByteArrayInputStream(data),
                                          data.length);
      });
    }
  }

  @Test
  void failsToCreateFilesInsideOtherFiles() {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt")) {
      var data = "foo".getBytes();
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());
      var fileChildPath = Paths.get(
          tmpFileFixture.tempFilePath.getFileName().toString(),
          "bar"
      ).toString();
      assertThrows(FileNotFoundException.class, () -> {
        fileController.updateFileContents(fileChildPath,
                                          new ByteArrayInputStream(data),
                                          data.length);
      });
    }
  }

  @Test
  void deletesExistingFiles() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var fileController =
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent());
      fileController.removeFile(tmpFileFixture.tempFilePath.getFileName().toString());
      assertThat(tmpFileFixture.tempFilePath.toFile().exists(),
                 is(false));
    }
  }
}
