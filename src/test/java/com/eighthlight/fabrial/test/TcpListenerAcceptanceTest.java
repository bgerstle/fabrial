package com.eighthlight.fabrial.test;

import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("acceptance")
public class TcpListenerAcceptanceTest {
  AppFixture appFixture;
  TcpClient client;

  @AfterEach
  void tearDown() throws Exception {
    if (appFixture != null) {
      appFixture.assertNoErrors();
      appFixture.stop();
      appFixture = null;
    }
    if (client != null) {
      client.close();
      client = null;
    }
  }

  @Test
  void whenStarted_thenHasNoErrors() throws Exception {
    appFixture = new AppFixture();
  }

  @Test
  void givenRunning_whenClientConnects_thenItSucceeds() throws IOException {
    appFixture = new AppFixture();
    client = new TcpClient();

    try {
      client.connect("localhost", 80, 1000);
    } catch (IOException e) {
      fail(e);
    }
  }
}
