package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.RequestParsingException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;

public class HttpRequestLineParsingTests {
  @Test
  void requestsWithoutHeadersOrBody() throws Exception {
    qt()
        .forAll(methods(), requestTargets(), httpVersions())
        .checkAssert((m, u, v) -> {
          String requestLine = m.name() + " "
                               + u.toString()+ " "
                               + "HTTP/" + v
                               + "\r\n";
          InputStream is = new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));
          Object req;
          try {
            req = Request.readFrom(is);
          } catch (IOException | RequestParsingException e) {
            req = e;
          }
          assertThat(req,
                     equalTo(new Request(v, m, u)));
        });
  }

  @Test
  void trailingWhitespaceIsIgnored() throws Exception {
    URI uri = new URI("/foo");
    String version = HttpVersion.ONE_ONE;

    InputStream is = new ByteArrayInputStream(
          ("GET " + uri.toString() + " HTTP/" + version + " \r\n"
        ).getBytes(StandardCharsets.UTF_8));

    assertThat(Request.readFrom(is),
               equalTo(new Request(version, Method.GET, uri)));
  }


  @Test
  void leadingWhitespaceCausesError() throws Exception{
    InputStream is = new ByteArrayInputStream(" GET / HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
    assertThrows(RequestParsingException.class, () -> {
      Request.readFrom(is);
    });
  }

  @Test
  void missingURI() {
    InputStream is = new ByteArrayInputStream("GET HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
    assertThrows(RequestParsingException.class, () -> {
      Request.readFrom(is);
    });
  }

  @Test
  void missingHttpVersion() {
    InputStream is = new ByteArrayInputStream("GET / HTTP/\r\n".getBytes(StandardCharsets.UTF_8));
    assertThrows(RequestParsingException.class, () -> {
      Request.readFrom(is);
    });
  }
}
