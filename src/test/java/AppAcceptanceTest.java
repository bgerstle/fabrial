import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AppAcceptanceTest {
  Process appProcess;

  @BeforeEach
  void setUp() throws IOException {
    appProcess = new ProcessBuilder("java", "-jar", "./build/libs/fabrial*.jar").start();
  }

  @AfterEach
  void tearDown() {
    appProcess.destroy();
  }

  @Test
  void clientConnectsToAppServer() throws IOException {
    TcpClient client = new TcpClient(new InetSocketAddress(8080));
    client.connect(2000);
  }
}
