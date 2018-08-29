import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TcpServer implements Closeable {
  // TODO: make logger instance-specific, prefixing logs w/ config & object info
  static final Logger logger = Logger.getLogger(TcpServer.class.getName());

  // configuration used to initialize the serverSocket
  final ServerConfig config;

  // number of clients currently connected to the server socket
  private final AtomicInteger connectionCount;

  // executor for handling of new connections
  private final ExecutorService connectionHandlerExecutor;

  // Thread where new connections are accepted from serverSocket
  private Optional<Thread> acceptThread;

  // socket which manages client connections
  // Initialized to empty, and set to a Socket object once started.
  private Optional<ServerSocket> serverSocket;

  TcpServer(ServerConfig config) {
    this.config = config;
    this.serverSocket = Optional.empty();
    this.acceptThread = Optional.empty();
    this.connectionCount = new AtomicInteger(0);
    this.connectionHandlerExecutor = Executors.newFixedThreadPool(this.config.maxConnections);
  }

  /**
   * @see connectionCount
    */
  int getConnectionCount() {
    return connectionCount.get();
  }

  /**
   * Start listening on the specified port.
   *
   * Must not be called more than once.
   *
   * @throws IOException If the underlying socket couldn't bind successfully.
   */
  void start() throws IOException {
    assert !serverSocket.isPresent();
    ServerSocket socket = new ServerSocket(this.config.port, this.config.maxConnections);
    this.serverSocket = Optional.of(socket);
    Thread acceptThread = new Thread(this::acceptConnections);
    acceptThread.start();
    this.acceptThread = Optional.of(acceptThread);
  }

  // Accept and handle new connections
  // Must be called on listenExecutor
  private void acceptConnections() {
    assert serverSocket.isPresent();
    ServerSocket serverSocket = this.serverSocket.get();
    while (!serverSocket.isClosed()) {
      try {
        Socket clientConnection = serverSocket.accept();
        connectionHandlerExecutor.execute(() -> handleConnection(clientConnection));
      } catch (IOException e) {
        logger.log(Level.FINE,
                   "Exception while accepting new connection",
                   e);
      }
    }
  }

  private void handleConnection(Socket connection) {
    this.connectionCount.incrementAndGet();
    try {
      connection.setSoTimeout(this.config.readTimeout);
      InputStream stream = connection.getInputStream();
      while (connection.isConnected()) {
        int b = stream.read();
        if (b == -1) {
          // EOF
          break;
        }
        // TODO: write b to outstream
      }
    } catch (IOException e) {
      logger.log(Level.FINE,
              "Exception while handling connection to "  + connection.getInetAddress(),
                 e);
    }

    try {
      connection.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
              "Connection to "
                     + connection.getInetAddress()
                     + " encountered exception while closing",
                 e);
    }
    this.connectionCount.decrementAndGet();
  }

  boolean isClosed() {
    return serverSocket.map(ServerSocket::isClosed).orElse(true);
  }

  public void close() throws IOException {
    if (this.serverSocket.isPresent()) {
      this.serverSocket.get().close();
    } else {
      logger.info("Server socket was absent, missing call to start.");
    }
  }
}
