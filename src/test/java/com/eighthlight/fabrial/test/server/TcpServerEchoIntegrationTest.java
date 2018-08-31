package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.test.client.EchoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TcpServerEchoIntegrationTest extends TcpServerIntegrationTest {
  @BeforeEach
  void startAndConnect() throws IOException {
    server.start();
    client.connect();
  }

  @Test
  void testInputEqualToOutput() throws IOException  {
    try (OutputStream os = client.getOutputStream();
        InputStream is = client.getInputStream()) {
      assertThat(new EchoRequest(os, is).send("foo"), equalTo("foo"));
    }
  }
}
