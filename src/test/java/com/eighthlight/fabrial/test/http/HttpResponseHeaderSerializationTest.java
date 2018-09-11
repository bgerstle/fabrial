package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpResponseHeaderSerializationTest {
  @Test
  void optionResponse() throws IOException {
    var response =
        new Response(HttpVersion.ONE_ONE,
                     200,
                     null,
                     Map.of("Allow", "HEAD, OPTIONS"));
    try (var os = new ByteArrayOutputStream()) {
      response.writeTo(os);
      var responseStr = os.toString();
      assertThat(responseStr , is("HTTP/1.1 200 Allow: HEAD,OPTIONS \r\n"));
    }
  }
}
