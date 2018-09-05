package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.test.client.EchoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TcpServerEchoIntegrationTest extends TcpServerIntegrationTest {
  @BeforeEach
  void startAndConnect() throws IOException {
    server.start();
    client.connect();
  }

  //@Test
  void testEchoingString() throws IOException, ClassNotFoundException  {
    try (OutputStream os = client.getOutputStream();
        InputStream is = client.getInputStream()) {
      assertThat(new EchoRequest(os, is).send("foo"), equalTo("foo"));
    }
  }

  //@Test
  void testEchoingNumber() throws IOException, ClassNotFoundException  {
    try (OutputStream os = client.getOutputStream();
        InputStream is = client.getInputStream()) {
      assertThat(new EchoRequest(os, is).send(0), equalTo(0));
    }
  }

  //@Test
  void testEchoingArrayOfStrings() throws IOException, ClassNotFoundException  {
    ArrayList<String> inData = new ArrayList<>(List.of("bar", "baz"));
    try (OutputStream os = client.getOutputStream();
        InputStream is = client.getInputStream()) {
      assertThat(new EchoRequest(os, is).send(inData), equalTo(inData));
    }
  }
}
