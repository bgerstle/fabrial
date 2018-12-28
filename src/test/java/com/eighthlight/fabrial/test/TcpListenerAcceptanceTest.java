package com.eighthlight.fabrial.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("acceptance")
public class TcpListenerAcceptanceTest {
  ServerProcess serverProcess;
  TcpClient client;

  @BeforeEach
  void setUp() throws Exception {
    serverProcess = new ServerProcess();
    client = TcpClient.forLocalServer();
  }

  @AfterEach
  void tearDown() throws Exception {
    if (serverProcess != null) {
      serverProcess.stop();
      serverProcess = null;
    }

    if (client != null) {
      client.close();
      client = null;
    }
  }

  @Test
  void whenStarted_thenHasNoErrors() throws IOException {
    assertEquals("Starting server...", serverProcess.readOutputLine());
    serverProcess.assertNoErrors();
  }

  @Test
  void givenRunning_whenClientConnects_thenItSucceeds() throws Exception {
    client.connect();
    assertTrue(client.isConnected());

    var echoInput = "foo";
    var response = client.echo(echoInput);
    assertEquals(echoInput, response);
  }
}
