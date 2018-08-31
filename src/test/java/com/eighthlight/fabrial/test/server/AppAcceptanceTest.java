package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.client.TcpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag("acceptance")
public class AppAcceptanceTest {
  static final Logger logger = Logger.getLogger(AppAcceptanceTest.class.getName());

  Process appProcess;

  @BeforeEach
  void setUp() throws IOException {
    // hard-coded to SNAPSHOT version. might need to fix this eventually...
    appProcess =
        new ProcessBuilder("java", "-jar", "./build/libs/fabrial-1.0-SNAPSHOT.jar")
            .inheritIO()
            .start();
  }

  @AfterEach
  void tearDown() {
    appProcess.destroy();
  }

  @Test
  void clientConnectsToAppServer() throws IOException {
    TcpClient client = new TcpClient(new InetSocketAddress(ServerConfig.DEFAULT_PORT));
    assertThat(() -> {
      try {
        client.connect(1000);
        return true;
      } catch (IOException e) {
        logger.info("Failed to connect (" + e.getMessage() + "). Retrying in 1s...");
        try {
          Thread.sleep(500);
        } catch (InterruptedException inte) {
          Thread.currentThread().interrupt();
        }
        return false;
      }
    }, eventuallyEval(is(true)));
  }
}
