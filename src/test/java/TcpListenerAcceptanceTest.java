import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TcpListenerAcceptanceTest {
  @Test
  void connect() throws IOException {
    // setup listener on port 80
    TcpClient client = new TcpClient(80, Optional.empty());
    client.connect(Optional.empty());
  }
}
