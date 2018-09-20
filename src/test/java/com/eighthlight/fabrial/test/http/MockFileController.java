package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.utils.Result;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MockFileController implements FileHttpResponder.FileController {
  MockDirectory root;

  private Optional<MockFsNode> fileAtPath(String relPathStr) {
    return relPathStr.matches("/?") ?
        Optional.of(root) : root.findChild(relPathStr);
  }

  @Override
  public boolean fileExistsAtPath(String relPathStr) {
    return fileAtPath(relPathStr).isPresent();
  }

  @Override
  public boolean isDirectory(String relPathStr) {
    return false;
  }

  @Override
  public List<String> getDirectoryContents(String relPathStr) {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.of((MockDirectory)f) : Optional.empty())
        .map(d -> d.children)
        .map(cs -> cs.stream().map(c -> c.getName()).collect(Collectors.toList()))
        .orElse(List.of());
  }

  @Override
  public long getFileSize(String relPathStr) {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .map(f -> f.data == null ? 0 : f.data.length)
        .map(Integer::toUnsignedLong)
        .orElse(0L);
  }

  @Override
  public String getFileMimeType(String relPathStr) throws IOException {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .map(f -> f.type)
        .orElse(null);
  }

  @Override
  public InputStream getFileContents(String relPathStr) throws IOException {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .map(f -> f.data)
        .map(ByteArrayInputStream::new)
        .orElse(null);
  }

  @Override
  public void updateFileContents(String relPathStr, InputStream data) throws IOException {
    var file = fileAtPath(relPathStr);
    fileAtPath(relPathStr)
        .flatMap(f -> !f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .ifPresent(f -> f.data = Result.attempt(data::readAllBytes).toOptional().orElse(null));
  }
}
