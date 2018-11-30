package com.eighthlight.fabrial.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class ServerConfig {
  public final int port;
  public final int readTimeout;
  public final Path directoryPath;
  public final Optional<Credential> adminCredential;
  public final int maxConnections;

  public static final int DEFAULT_PORT = 8080;
  public static final int DEFAULT_READ_TIMEOUT = 10000;
  public static final Path DEFAULT_DIRECTORY_PATH = Paths.get("").toAbsolutePath();

  public ServerConfig(int port,
                      int readTimeout,
                      Path directoryPath) {
    this(port, readTimeout, directoryPath, Optional.empty(), Runtime.getRuntime().availableProcessors());
  }

  public ServerConfig(int port,
                      int readTimeout,
                      Path directoryPath,
                      Optional<Credential> adminCredential,
                      int maxConnections) {
    this.port = port;
    this.readTimeout = readTimeout;
    this.directoryPath = directoryPath;
    this.adminCredential = adminCredential;
    this.maxConnections = maxConnections;
  }
}
