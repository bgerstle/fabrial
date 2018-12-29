package com.eighthlight.fabrial.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("acceptance")
public class EchoServerAcceptanceTest {
  ServerProcess serverProcess;

  @BeforeEach
  void setUp() throws Exception {
    serverProcess = new ServerProcess();
    serverProcess.assertNoErrors();
  }

  @Test
  void whenClientEchoes_thenItGetsIdenticalResponse() throws Exception {
    try (var client = TcpClient.forLocalServer()) {
      client.connect();

      var echoInput = "foo";
      var response = client.echo(echoInput);

      assertEquals(echoInput, response);
    }
  }

  @AfterEach
  void tearDown() throws Exception {
    serverProcess.stop();
  }
}
