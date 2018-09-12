package com.eighthlight.fabrial.http.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class FileResponderDataSourceImpl implements FileHttpResponder.DataSource {
  public final Path baseDirPath;

  public FileResponderDataSourceImpl(Path baseDirPath) {
    this.baseDirPath = Optional.ofNullable(baseDirPath).orElse(Paths.get("."));
  }

  @Override
  public boolean fileExistsAtPath(Path path) {
    return Paths.get(baseDirPath.toAbsolutePath().toString(), path.toString())
                .toFile()
                .exists();
  }

  @Override
  public boolean isDirectory(Path path) {
    return false;
  }

  @Override
  public List<Path> getDirectoryContents(Path path) {
  }
}
