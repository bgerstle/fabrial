package com.eighthlight.fabrial.http.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
  public InputStream getFileContents(String relPathStr) throws IOException {
    return new FileInputStream(absolutePathInBaseDir(relPathStr).toFile());
  }
}
