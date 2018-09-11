package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpHeaderWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpHeaderWriterSerializationTests {
  @Test
  void allowHEAD() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer = new HttpHeaderWriter(os);
      writer.writeField("Allow", "GET");
      assertThat(os.toString(), is("Allow: GET"));
    }
  }

  @Test
  void allowMultipleMethods() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer = new HttpHeaderWriter(os);
      writer.writeField("Allow", "GET,HEAD,OPTIONS");
      assertThat(os.toString(), is("Allow: GET,HEAD,OPTIONS"));
    }
  }

  @Test
  void writeMultipleFields() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer = new HttpHeaderWriter(os);
      writer.writeFields(Map.of("Allow", "GET,HEAD,OPTIONS", "Content-Length", "0"));
      assertThat(os.toString(), is("Allow: GET,HEAD,OPTIONS Content-Length: 0"));
    }
  }
}
