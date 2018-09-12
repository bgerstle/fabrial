package com.eighthlight.fabrial.http.file;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FileResponderDataSourceImpl implements FileHttpResponder.DataSource {
  public final Path baseDirPath;

  public FileResponderDataSourceImpl(Path baseDirPath) {
    this.baseDirPath = Optional.ofNullable(baseDirPath).orElse(Paths.get("."));
  }

  private Path absolutePathInBaseDir(Path path) {
    return Paths.get(baseDirPath.toAbsolutePath().toString(), path.toString());
  }

  @Override
  public boolean fileExistsAtPath(Path path) {
    return absolutePathInBaseDir(path).toFile().exists();
  }

  @Override
  public boolean isDirectory(Path path) {
    return absolutePathInBaseDir(path).toFile().isDirectory();
  }

  @Override
  public List<Path> getDirectoryContents(Path path) {
    return Arrays.asList(
        Arrays.asList(absolutePathInBaseDir(path).toFile().listFiles())
              .stream()
              .map(File::getPath)
              .map(Paths::get)
              .map(baseDirPath::relativize)
              .toArray(Path[]::new));
  }
}
