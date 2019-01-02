package com.eighthlight.fabrial.test.acceptance;

import com.eighthlight.fabrial.test.utils.ServerProcess;
import com.eighthlight.fabrial.test.utils.TcpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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

  @Test
  void givenRunning_whenConsecutiveEchoesAreSent_thenTheyReceiveResponses() {
    var echoInputs = List.of("foo", "bar", "baz");

    var echoResponses =
        echoInputs.stream()
                  .map(input -> {
                    try (var client = TcpClient.forLocalServer()) {
                      client.connect();
                      return client.echo(input);
                    } catch (Exception e) {
                      fail(e);
                    }
                    return null;
                  })
                  .collect(Collectors.toList());

    assertEquals(echoInputs, echoResponses);
  }
}
