package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.RequestParsingException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpRequestLineParsingTests {
  @Test
  void getWithoutHeaders()
      throws URISyntaxException, RequestParsingException, IOException {
    URI uri = new URI("/");
    InputStream is = new ByteArrayInputStream("GET / HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
    assertThat(Request.readFrom(is),
               equalTo(new Request("1.1", Method.GET, new URI("/"))));
  }

  @Test
  void leadingWhitespace() {

  }

  @Test
  void trailingWhitespace() {

  }


  @Test
  void missingURI() {

  }

  @Test
  void missingHttpVersion() {

  }

  @Test
  void unsupportedHttpVersion() {

  }
}
