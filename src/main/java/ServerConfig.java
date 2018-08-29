class ServerConfig {
  final int port;
  final int maxConnections;
  final int readTimeout;

  static final int DEFAULT_PORT = 80;
  static final int DEFAULT_MAX_CONNECTIONS = 50;
  static final int DEFAULT_READ_TIMEOUT = 10000;

  ServerConfig() {
    this(DEFAULT_PORT, DEFAULT_MAX_CONNECTIONS, DEFAULT_READ_TIMEOUT);
  }

  ServerConfig(int port) {
    this(port, DEFAULT_MAX_CONNECTIONS, DEFAULT_READ_TIMEOUT);
  }

  ServerConfig(int port, int maxConnections, int readTimeout) {
    this.port = port;
    this.maxConnections = maxConnections;
    this.readTimeout = readTimeout;
  }
}
