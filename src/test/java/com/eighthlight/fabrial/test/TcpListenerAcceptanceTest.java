package com.eighthlight.fabrial.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("acceptance")
public class TcpListenerAcceptanceTest {
  ServerProcess serverProcess;
  TcpClient client;

  @AfterEach
  void tearDown() throws Exception {
    if (serverProcess != null) {
      serverProcess.assertNoErrors();
      serverProcess.stop();
      serverProcess = null;
    }

    if (client != null) {
      client.close();
      client = null;
    }
  }

  @Test
  void whenStarted_thenHasNoErrors() throws Exception {
    serverProcess = new ServerProcess();
  }

  @Test
  void givenRunning_whenClientConnects_thenItSucceeds() throws IOException {
    serverProcess = new ServerProcess();

    try {
      client = new TcpClient("localhost", 80, 1000);
    } catch (IOException e) {
      fail(e);
    }
  }

  @Test
  void givenHasClientConnection_whenAnotherClientConnects_thenItSucceeds() throws IOException {
    serverProcess = new ServerProcess();

    try {
      client = new TcpClient("localhost", 80, 1000);
      var client2 = new TcpClient("localhost", 80, 1000);

      client2.close();
    } catch (IOException e) {
      fail(e);
    }
  }
}
