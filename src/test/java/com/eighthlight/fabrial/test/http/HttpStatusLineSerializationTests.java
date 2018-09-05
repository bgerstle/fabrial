package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpStatusLineSerializationTests {
  @Test
  void successfulResponse() throws Exception {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    new Response("1.1", 200, null).writeTo(os);
    String line = new String(os.toByteArray(), StandardCharsets.UTF_8);
    assertThat(line,
               equalTo("HTTP/1.1 200 \r\n"));
  }
}
