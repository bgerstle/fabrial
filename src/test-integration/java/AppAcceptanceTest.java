import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public class AppAcceptanceTest {
  Process appProcess;

  @BeforeEach
  void setUp() throws IOException {
    // hard-coded to SNAPSHOT version. might need to fix this eventually...
    appProcess =
        new ProcessBuilder("java", "-jar", "./build/libs/fabrial-1.0-SNAPSHOT.jar")
            .start();
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
