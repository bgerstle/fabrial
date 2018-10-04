package com.eighthlight.fabrial.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ServerConfig {
  public final int port;
  public final int readTimeout;
  public final Path directoryPath;
  public final Optional<AdminCredentials> adminCredentials;

  public static final int DEFAULT_PORT = 8080;
  public static final int DEFAULT_READ_TIMEOUT = 10000;
  public static final Path DEFAULT_DIRECTORY_PATH = Paths.get("").toAbsolutePath();

  public ServerConfig(int port,
                      int readTimeout,
                      Path directoryPath) {
    this(port, readTimeout, directoryPath, Optional.empty());
  }

  public ServerConfig(int port,
                      int readTimeout,
                      Path directoryPath,
                      Optional<AdminCredentials> adminCredentials) {
    this.port = port;
    this.readTimeout = readTimeout;
    this.directoryPath = directoryPath;
    this.adminCredentials = adminCredentials;
  }
}
