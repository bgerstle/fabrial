package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import static com.eighthlight.fabrial.http.HttpVersion.ONE_ONE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpHeadIntegrationTest extends TcpServerIntegrationTest {
  @BeforeEach
  void startAndConnect() throws IOException {
    server.start();
    client.connect();
  }

  @Test
  void simpleHEADRequest() throws Throwable {
    Request.builder()
           .withVersion(ONE_ONE)
           .withMethod(Method.HEAD)
           .withUriString("/test")
           .build()
           .writeTo(client.getOutputStream());

    BufferedReader reader = new BufferedReader(new InputStreamReader((client.getInputStream())));
    String response = reader.readLine();
    assertThat(response, containsString("200"));
  }
}
