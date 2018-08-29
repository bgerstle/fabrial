import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class TcpServer {
  // configuration used to initialize the serverSocket
  final ServerConfig config;

  // number of connections to the server socket
  private final AtomicInteger connectionCount;

  // executor for scheduling the handling of new connections
  private final Executor connectionHandlerExecutor;

  // executor for managing the server serverSocket
  private final Executor listenExecutor;

  // socket which manages client connections
  // !!!: All modifications should be performed via listenerExecutor
  // TODO: wrap in
  // Initialized to empty, and set to a Socket object once started.
  private Optional<ServerSocket> serverSocket;

  TcpServer(ServerConfig config) {
    this.config = config;
    this.serverSocket = Optional.empty();
    this.listenExecutor = Executors.newSingleThreadExecutor();
    this.connectionCount = new AtomicInteger(0);
    this.connectionHandlerExecutor = Executors.newFixedThreadPool(this.config.maxConnections);
  }

  int getConnectionCount() {
    return connectionCount.get();
  }

  private void resetConnections() {
    connectionCount.set(0);
  }

  void start() throws IOException {
    assert !serverSocket.isPresent();
    ServerSocket socket = new ServerSocket(this.config.port, this.config.maxConnections);
    this.serverSocket = Optional.of(socket);
    listenExecutor.execute(this::listen);
  }

  private void listen() {
    Optional<Socket> connection = accept();
    if(connection.isPresent()) {
      Socket clientSocket = connection.get();
      connectionHandlerExecutor.execute(() -> handleConnection(clientSocket));
      this.listenExecutor.execute(this::listen);
    }
  }

  private Optional<Socket> accept() {
    return this.serverSocket
        .map((serverSocket) -> {
          try {
            return serverSocket.accept();
          } catch (IOException e) {
            e.printStackTrace();
            return null;
          }
        });
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
      e.printStackTrace();
    }
    this.connectionCount.decrementAndGet();
  }

  void stop() {
    this.listenExecutor.execute(() -> {
      try {
        close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private void close() throws IOException {
    if (this.serverSocket.isPresent()) {
      this.serverSocket.get().close();
      this.serverSocket = Optional.empty();
    }
  }
}
