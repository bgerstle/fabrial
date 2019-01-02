package com.eighthlight.fabrial.test;

import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Tag("acceptance")
public class EchoServerAcceptanceTest {
  ServerProcess serverProcess;

  @BeforeEach
  void setUp() throws IOException {
    serverProcess = ServerProcess.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    serverProcess.stop();
    serverProcess.awaitTermination(2, TimeUnit.SECONDS);
  }

  @Test
  public void whenClientSendsData_thenItReceivesSameDataBack() throws Exception {
    try (var client = TcpClient.forLocalServer()) {
      client.connect(3);

      var exampleInputData = "foo".getBytes();
      var echoResponse = client.echo(exampleInputData);

      assertArrayEquals(exampleInputData, echoResponse);
    }
  }
}
