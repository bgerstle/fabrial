package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.response.HttpHeaderWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class HttpHeaderWriterSerializationTests {
  @Test
  void allowHEAD() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer = new HttpHeaderWriter(os);
      writer.writeFields(Map.of("Allow", "GET"));
      assertThat(os.toString(), is("Allow: GET " + CRLF));
    }
  }

  @Test
  void allowMultipleMethods() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer = new HttpHeaderWriter(os);
      writer.writeFields(Map.of("Allow", "GET,HEAD,OPTIONS"));
      assertThat(os.toString(), is("Allow: GET,HEAD,OPTIONS " + CRLF));
    }
  }

  @Test
  void writeMultipleFields() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer  new HttpHeaderWriter(os);
      writer.writeFields(Map.of(
          "Allow", "GET,HEAD,OPTIONS",
          "Content-Length", "0"));
      var expectedAllow = "Allow: GET,HEAD,OPTIONS ";
      var expectedAllow = "Allow: GET,HEAD,OPTIONS";
      var expectedContentLength = "Content-Length: 0";
      var headerLines = Arrays.asList(os.toString().split(CRLF));
      assertThat(headerLines, containsInAnyOrder(expectedAllow, expectedContentLength));
    }
  }
}
