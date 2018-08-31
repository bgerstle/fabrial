package com.eighthlight.fabrial.server;

public class ServerConfig {
  public final int port;
  public final int readTimeout;

  public static final int DEFAULT_PORT = 8080;
  public static final int DEFAULT_READ_TIMEOUT = 10000;

  public ServerConfig() {
    this(DEFAULT_PORT, DEFAULT_READ_TIMEOUT);
  }

  public ServerConfig(int port) {
    this(port, DEFAULT_READ_TIMEOUT);
  }

  public ServerConfig(int port, int readTimeout) {
    this.port = port;
    this.readTimeout = readTimeout;
  }
}
