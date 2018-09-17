package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.HttpHeaderReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Map;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.headers;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.optionalWhitespace;
import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.core.Is.is;
import static org.quicktheories.QuickTheory.qt;

public class HttpHeaderReaderTest {
  public static String headerLineFromComponents(Map<String, String> headers,
                                                String ows1,
                                                String ows2) {
    var lineBuilder = new StringBuilder();
    for (var entry : headers.entrySet()) {
      lineBuilder.append(entry.getKey());
      lineBuilder.append(":");
      lineBuilder.append(ows1);
      lineBuilder.append(entry.getValue());
      lineBuilder.append(ows2);
      lineBuilder.append(CRLF);
    }
    return lineBuilder.toString();
  }

  public static Readable sourceFromString(String str) {
    return new InputStreamReader(new ByteArrayInputStream(str.getBytes()));
  }

  @Test
  void parsesNameAndValueOfHeaderLine() {
    var headerLines = sourceFromString("Content-Type: text/plain" + CRLF);

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

    var headerReader = new HttpHeaderReader(sourceFromString(headerLines));
    var headers = headerReader.readHeaders();
    assertThat(headers,
               is(Map.of(
                   "Content-Type", "text/plain; charset=utf-8",
                   "Content-Length", "5"
               )));
  }

  @Test
  void noHeaders() {
    var headerReader = new HttpHeaderReader(sourceFromString(CRLF));
    var headers = headerReader.readHeaders();
    assertThat(headers, is(emptyMap()));
  }

  @Test
  void missingFieldName() {
    var headerReader = new HttpHeaderReader(sourceFromString(": foo" + CRLF));
    var headers = headerReader.readHeaders();
    assertThat(headers, is(emptyMap()));
  }

  @Test
  void missingFieldValue() {
    var headerReader = new HttpHeaderReader(sourceFromString("foo:" + CRLF));
    var headers = headerReader.readHeaders();
    assertThat(headers, is(Map.of("foo", "")));
  }

  @Test
  void allWhitespace() {
    var headerReader = new HttpHeaderReader(sourceFromString(" :   " + CRLF));
    var headers = headerReader.readHeaders();
    assertThat(headers, is(emptyMap()));
  }

  @Test
  void doesNotConsumeEntireStream() throws IOException {
    var headerLines = String.join(CRLF, List.of(
        "Accept: */*"
    ))
    // add newline and body
    + CRLF + "body";
    var source = sourceFromString(headerLines);
    var headerReader = new HttpHeaderReader(source);
    var headers = headerReader.readHeaders();
    assertThat(headers, is(Map.of("Accept", "*/*")));
    var cb = CharBuffer.allocate(0);
    assertThat(source.read(cb), greaterThanOrEqualTo(1));
  }

  @Test
  void arbitraryHeaderLines() {
    qt().forAll(headers(), optionalWhitespace(), optionalWhitespace())
        .asWithPrecursor(HttpHeaderReaderTest::headerLineFromComponents)
        .checkAssert((headers, ows1, ows2, headerLines) -> {
          var reader = new HttpHeaderReader(sourceFromString(headerLines));
          assertThat(reader.readHeaders(), is(headers));
        });
  }
}
