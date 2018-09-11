package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpHeaderWriter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;

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
      writer.writeFields(Map.of(
          "Allow", "GET,HEAD,OPTIONS",
          "Content-Length", "0"));
      var expectedAllow = "Allow: GET,HEAD,OPTIONS";
      var expectedContentLength = "Content-Length: 0";
      var headerFieldStr = os.toString();
      assertThat(headerFieldStr, allOf(
          containsString(expectedAllow),
          containsString(expectedContentLength)
      ));
      headerFieldStr =
          StringUtils.replace(headerFieldStr, expectedAllow, "", 1);

      headerFieldStr =
          StringUtils.replace(headerFieldStr, expectedContentLength, "", 1);

      assertThat(headerFieldStr, is(" "));
    }
  }
}
