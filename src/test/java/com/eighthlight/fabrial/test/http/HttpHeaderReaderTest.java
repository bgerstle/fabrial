package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.HttpHeaderReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.headers;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.optionalWhitespace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.quicktheories.QuickTheory.qt;

public class HttpHeaderReaderTest {
  @Test
  void parsesNameAndValueOfHeaderLine() {
    var headerLines =
        new ByteArrayInputStream(("Content-Type: text/plain" + CRLF).getBytes());

    var headerReader = new HttpHeaderReader(headerLines);

    assertThat(headerReader.nextFieldName(), is("Content-Type"));
    assertThat(headerReader.nextFieldValue(), is("text/plain"));
  }

  @Test
  void multipleHeaders() {
    var headerLines =
        String.join(CRLF, List.of(
            "Content-Type: text/plain; charset=utf-8",
            "Content-Length: 5"
        ))
        // add newline which would precede the request body
        + CRLF;

    var headerReader = new HttpHeaderReader(new ByteArrayInputStream(headerLines.getBytes()));
    var headers = headerReader.readHeaders();
    assertThat(headers,
              is(Map.of(
                  "Content-Type", "text/plain; charset=utf-8",
                  "Content-Length", "5"
              )));
  }

  @Test
  void arbitraryHeaderLines() {
    qt().forAll(headers(), optionalWhitespace(), optionalWhitespace())
        .checkAssert((headers, ows1, ows2) -> {
      var lineBuilder = new StringBuilder();
      for (var entry: headers.entrySet()) {
        lineBuilder.append(entry.getKey());
        lineBuilder.append(":");
        lineBuilder.append(ows1);
        lineBuilder.append(entry.getValue());
        lineBuilder.append(ows2);
        lineBuilder.append(CRLF);
      }
      var headerLines = lineBuilder.toString();
      var reader = new HttpHeaderReader(new ByteArrayInputStream(headerLines.getBytes()));
      assertThat(reader.readHeaders(), is(headers));
    });
  }
}
