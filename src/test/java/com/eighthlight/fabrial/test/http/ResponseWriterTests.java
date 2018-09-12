package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.maps;
import static org.quicktheories.generators.SourceDSL.strings;

public class ResponseWriterTests {
  private static Gen<ByteArrayInputStream> bodyStreams(int length) {
    return strings()
        .allPossible()
        .ofLengthBetween(1, length)
        .map(s -> {
          return Result.attempt(() ->
                                    new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8))
          ).orElseAssert();
        });
  }

  private static Gen<Response> responses() {
    return httpVersions()
        .zip(statusCodes(),
             responseReasons(10).toOptionals(20),
             maps().of(alphanumeric(16), alphanumeric(16))
                   .ofSizeBetween(1, 5).toOptionals(50),
             bodyStreams(10).toOptionals(10),
             (version, statusCode, reason, headers, body) -> {
               return new Response(version,
                                   statusCode,
                                   reason.orElse(null),
                                   headers.orElse(null),
                                   body.orElse(null));
             });
  }
  @Test
  void arbitraryResponseSerialization() {
    qt().forAll(responses()).checkAssert((response) -> {
      try (var os = new ByteArrayOutputStream()) {
        Result.attempt(() -> new ResponseWriter(os).writeResponse(response)).orElseAssert();
        var responseLines = os.toString().split(CRLF);
        assertThat(responseLines[0],
                   is("HTTP/"
                      + response.version
                      + " "
                      + response.statusCode
                      + " "
                      + Optional.ofNullable(response.reason).orElse("")));

        if (response.headers == null && response.body == null) {
          assertThat(responseLines.length, equalTo(1));
          return;
        }

        List<String> otherLines =
            Arrays.asList(responseLines).subList(1, responseLines.length);

        List<String> headerLines = otherLines.subList(0, response.headers.size());

        assertThat(headerLines.isEmpty(), equalTo(response.headers.isEmpty()));
        response.headers.entrySet()
                        .stream()
                        .map(e -> e.getKey() + ": " + e.getValue() + " ")
                        .forEach(s -> assertThat(headerLines, hasItem(s)));
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    });
  }
}
