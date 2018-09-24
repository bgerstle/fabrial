package com.eighthlight.fabrial.http.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.toIntExact;

public class LocalFilesystemController implements FileHttpResponder.FileController {
  private static final Logger logger = LoggerFactory.getLogger(LocalFilesystemController.class);

  public final Path baseDirPath;

  public LocalFilesystemController(Path baseDirPath) {
    this.baseDirPath = Optional.ofNullable(baseDirPath).orElse(Paths.get("."));
  }

  private Path absolutePathInBaseDir(String relPathStr) {
    return Paths.get(baseDirPath.toAbsolutePath().toString(), relPathStr.toString());
  }

  @Override
  public boolean fileExistsAtPath(String relPathStr) {
    return absolutePathInBaseDir(relPathStr).toFile().exists();
  }

  @Override
  public boolean isDirectory(String relPathStr) {
    return absolutePathInBaseDir(relPathStr).toFile().isDirectory();
  }

  @Override
  public List<String> getDirectoryContents(String relPathStr) {
    return Optional
        .ofNullable(absolutePathInBaseDir(relPathStr).toFile().listFiles())
        .map(Arrays::asList)
        .map(List::stream)
        .map(s -> s.map(File::getPath)
                   .map(Paths::get)
                   .map(baseDirPath::relativize)
                   .map(Path::toString)
                   .toArray(String[]::new))
        .map(Arrays::asList)
        .orElse(null);
  }

  @Override
  public long getFileSize(String relPathStr) {
    return absolutePathInBaseDir(relPathStr).toFile().length();
  }

  @Override
  public InputStream getFileContents(String relPathStr, int offset, int length) throws IOException {
    var file = absolutePathInBaseDir(relPathStr).toFile();
    var fis = new FileInputStream(file);
    fis.skip(offset);
    if (length == toIntExact(file.length()) - offset) {
      // if reading the rest of the file, just return the stream
      return fis;
    }
    // if reading portion of the file, do so and return as a wrapped buffer
    var buf = ByteBuffer.allocate(length);
    var actualRead = fis.read(buf.array(), 0, length);
    if (actualRead != length) {
      throw new IOException(
          String.format("Failed to read %d bytes at %d offset", length, offset)
      );
    }
    return new ByteArrayInputStream(buf.array());
  }

  @Override
  public String getFileMimeType(String relPathStr) throws IOException {
    return Files.probeContentType(absolutePathInBaseDir(relPathStr));
  }

  @Override
  public void updateFileContents(String relPathStr, InputStream data, int length) throws IOException {
    var file = absolutePathInBaseDir(relPathStr).toFile();
    try (var fileOutStream = new FileOutputStream(file)) {
      // TODO: surely there's a way to "transfer N bytes w/ a buffer"...
      // of course, without messing up the actual data w/ an underlying string encoder
      var buf = ByteBuffer.allocate(length);
      data.read(buf.array(), 0, length);
      fileOutStream.write(buf.array(), 0, length);
    }
  }

  @Override
  public void removeFile(String relPathStr) throws IOException {
    var deleted = absolutePathInBaseDir(relPathStr).toFile().delete();
    if (!deleted) {
      throw new IOException("Failed to delete file");
    }
  }
}
