package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.HttpHeaderReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

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
    var headerLines = String.join(CRLF, List.of(
        "Content-Type: text/plain",
        "Content-Length: 5"
    ));

    var headerReader = new HttpHeaderReader(new ByteArrayInputStream(headerLines.getBytes()));

    assertThat(headerReader.nextFieldName(), is("Content-Type"));
    assertThat(headerReader.nextFieldValue(), is("text/plain"));
    headerReader.skipToNextLine();
    assertThat(headerReader.nextFieldName(), is("Content-Length"));
    assertThat(headerReader.nextFieldValue(), is("5"));
  }
}
