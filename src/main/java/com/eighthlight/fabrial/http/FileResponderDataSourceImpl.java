package com.eighthlight.fabrial.http;

import java.nio.file.Path;

public class FileResponderDataSourceImpl implements FileHttpResponder.DataSource {
  @Override
  public boolean fileExistsAtPath(Path path) {
    return path.toFile().exists();
  }
}
