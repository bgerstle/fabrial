import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Optional;

public class TcpClient implements Closeable {
  final String host;
  final int port;
  Optional<Socket> socket;

  TcpClient(int port, Optional<String> host) {
    this.host = host.orElse("localhost");
    this.port = port;
    this.socket = Optional.empty();
  }

  void connect(Optional<Integer> timeout) throws IOException {
    assert !this.socket.isPresent();
    try (Socket socket = new Socket()) {
      SocketAddress address = new InetSocketAddress(this.host, this.port);
      socket.connect(address, timeout.orElse(0));
      this.socket = Optional.of(socket);
    }
  }

  @Override
  public void close() throws IOException {
    if (this.socket.isPresent()) {
      this.socket.get().close();
    }
  }
}
