package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.response.HttpHeaderWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.maps;

public class HttpResponseHeaderSerializationTest {
  private static String serializeHeaders(Map<String, String> headers) throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      new HttpHeaderWriter(os).writeFields(headers);
      return os.toString();
    }
  }

  @Test
  void serializesOneHeader() throws IOException {
    var responseStr = serializeHeaders(Map.of("Allow", "HEAD,OPTIONS"));
    assertThat(responseStr , is("Allow: HEAD,OPTIONS " + CRLF));
  }

  @Test
  void responseWithTwoHeaders() throws IOException {
    var headerLines =
        Arrays.asList(
            serializeHeaders(Map.of("Allow", "HEAD,OPTIONS",
                                    "Content-Length", "0"))
                .split(CRLF));
    assertThat(headerLines,
               containsInAnyOrder("Allow: HEAD,OPTIONS ", "Content-Length: 0 "));
  }

  @Test
  void arbitraryHeaders() {
    qt().forAll(maps().of(alphanumeric(16), alphanumeric(16))
                      .ofSizeBetween(1, 5))
        .checkAssert(headers -> {
          var headerLines = Result.attempt(() -> serializeHeaders(headers))
                                  .map(s -> Arrays.asList(s.split(CRLF)))
                                  .orElseAssert();;
          headers.entrySet()
                 .stream()
                 .map(e -> e.getKey() + ": " + e.getValue() + " ")
                 .forEach(s -> assertThat(headerLines, hasItem(s)));
        });
  }
}
