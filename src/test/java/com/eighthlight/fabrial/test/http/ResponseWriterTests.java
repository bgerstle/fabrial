package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Response;
import com.eighthlight.fabrial.http.ResponseWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.maps;

public class ResponseWriterTests {
  @Test
  void arbitraryResponseSerialization() {
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
            Result.attempt(() -> new ResponseWriter(os).writeResponse(response)).orElseAssert();
            var responseLines = os.toString().split(CRLF);
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
