package com.eighthlight.fabrial.server;

public class ServerConfig {
  public final int port;
  public final int maxConnections;
  public final int readTimeout;

  public static final int DEFAULT_PORT = 8080;
  public static final int DEFAULT_MAX_CONNECTIONS = 50;
  public static final int DEFAULT_READ_TIMEOUT = 10000;

  public ServerConfig() {
    this(DEFAULT_PORT, DEFAULT_MAX_CONNECTIONS, DEFAULT_READ_TIMEOUT);
  }

  public ServerConfig(int port) {
    this(port, DEFAULT_MAX_CONNECTIONS, DEFAULT_READ_TIMEOUT);
  }

  public ServerConfig(int port, int maxConnections, int readTimeout) {
    this.port = port;
    this.maxConnections = maxConnections;
    this.readTimeout = readTimeout;
  }
}
