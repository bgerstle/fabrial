package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.HttpHeaderReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class HttpHeaderReaderTest {
  @Test
  void parsesKeyOfHeaderLine() {
    var headerLines =
        new ByteArrayInputStream(("Content-Type: text/plain" + CRLF).getBytes());

    var headerReader = new HttpHeaderReader(headerLines);

    assertThat(headerReader.getKey(), is("Content-Type"));
  }
}
