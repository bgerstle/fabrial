import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;

public class TcpClient implements Closeable {
  final SocketAddress address;
  Optional<Socket> socket;

  TcpClient(SocketAddress address) {
    this.address = address;
    this.socket = Optional.empty();
  }

  void connect() throws IOException {
    connect(0);
  }

  void connect(int timeout) throws IOException {
    assert !this.socket.isPresent();
    Socket socket = new Socket();
    socket.connect(address, timeout);
    this.socket = Optional.of(socket);
  }

  boolean isClosed() {
    return socket.map(Socket::isClosed).orElse(true);
  }

  @Override
  public void close() throws IOException {
    if (this.socket.isPresent()) {
      this.socket.get().close();
    }
  }
}
