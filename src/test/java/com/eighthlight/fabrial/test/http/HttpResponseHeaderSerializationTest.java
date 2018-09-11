package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Response;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.maps;

public class HttpResponseHeaderSerializationTest {
  @Test
  void responseWithOneHeader() throws IOException {
    var response =
        new Response(HttpVersion.ONE_ONE,
                     200,
                     null,
                     Map.of("Allow", "HEAD,OPTIONS"));
    try (var os = new ByteArrayOutputStream()) {
      response.writeTo(os);
      var responseStr = os.toString();
      assertThat(responseStr , is("HTTP/1.1 200 \r\nAllow: HEAD,OPTIONS \r\n\r\n"));
    }
  }

  @Test
  void responseWithTwoHeaders() throws IOException {
    var response =
        new Response(HttpVersion.ONE_ONE,
                     200,
                     null,
                     Map.of(
                         "Allow", "HEAD,OPTIONS",
                         "Content-Length", "0"));
    try (var os = new ByteArrayOutputStream()) {
      response.writeTo(os);
      var responseLines = os.toString().split("\r\n");
      assertThat(responseLines[0], is("HTTP/1.1 200 "));
      assertThat(List.of(responseLines[1], responseLines[2]),
                 containsInAnyOrder("Allow: HEAD,OPTIONS ", "Content-Length: 0 "));
    }
  }

  @Test
  void responseWithArbitraryHeaders() {
    qt().forAll(httpVersions(),
                statusCodes(),
                responseReasons(10),
                maps().of(alphanumeric(16), alphanumeric(16))
                      .ofSizeBetween(1, 5))
        .checkAssert((version, statusCode, reason, headerFields) -> {
          var response = new Response(version,
                                      statusCode,
                                      reason,
                                      headerFields);
          try (var os = new ByteArrayOutputStream()) {
            Result.attempt(() -> response.writeTo(os)).orElseAssert();
            var responseLines = os.toString().split("\r\n");
            assertThat(responseLines[0],
                       is("HTTP/"
                          + version
                          + " "
                          + statusCode
                          + " "
                          + reason));
            List<String> otherLines =
                Arrays.asList(responseLines).subList(1, responseLines.length);
            headerFields.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ": " + e.getValue() + " ")
                        .forEach(s -> assertThat(otherLines, hasItem(s)));
          } catch (IOException e) {
            throw new AssertionError(e);
          }
        });
  }
}
