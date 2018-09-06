package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.RequestParsingException;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.constant;
import static org.quicktheories.generators.Generate.oneOf;
import static org.quicktheories.generators.Generate.pick;
import static org.quicktheories.generators.SourceDSL.lists;
import static org.quicktheories.generators.SourceDSL.strings;

public class HttpRequestLineParsingTests {
  public static Gen<String> invalidMethods() {
    return oneOf(constant("FOO"), constant("BAR"), constant("BAZ"));
  }

  public static Gen<String> invalidUris() {
    return nonAsciiStrings();
  }

  public static Gen<String> invalidVersions() {
    return pick(List.of("0.8", "2.6", "0.0"));
  }

  public static ByteArrayInputStream requestLineFromComponents(
      String method,
      String uri,
      String version) {
    String requestLine = method + " "
                         + uri + " "
                         + "HTTP/" + version
                         + "\r\n";
    return new ByteArrayInputStream(requestLine.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void requestsWithoutHeadersOrBody() {
    qt().forAll(methods(), requestTargets(), httpVersions())
        .checkAssert((m, u, v) -> {
          Object req;
          try {
            req = Request.builder().buildWithStream(requestLineFromComponents(m.name(), u.toString(), v));
          } catch (RequestParsingException e) {
            req = e;
          }
          assertThat(req,
                     equalTo(Request.builder().withVersion(v).withMethod(m).withUri(u).build()));
        });
  }

  @Test
  void throwsWithMissingComponents() {
    qt().forAll(methods().toOptionals(75),
                requestTargets().toOptionals(75),
                httpVersions().toOptionals(75))
        .assuming((m, u, v) ->
            // make sure either method or version are absent
            // uri is allowed to be empty
          !(m.isPresent() && v.isPresent())
        )
        .checkAssert((m, u, v) -> {
          ByteArrayInputStream is =
              requestLineFromComponents(
                  m.map(Method::name).orElse(""),
                  u.map(URI::toString).orElse(""),
                  v.orElse(""));
      assertThrows(RequestParsingException.class, () -> {
        Request.builder().buildWithStream(is);
      });
    });
  }

  @Test
  void throwsWithBadTargets() {
    qt().forAll(invalidMethods(), invalidUris(), httpVersions())
        .checkAssert((m, u, v) -> {
          assertThrows(RequestParsingException.class, () -> {
            Request.builder().buildWithStream(requestLineFromComponents(m, u, v));
          });
        });
  }

  @Test
  void throwsWithBadVersions() {
    qt().forAll(methods(), requestTargets(), invalidVersions())
        .checkAssert((m, u, v) -> {
          assertThrows(RequestParsingException.class, () -> {
            Request.builder().buildWithStream(requestLineFromComponents(m.name(), u.toString(), v));
          });
        });
  }

  @Test
  void trailingWhitespaceIsIgnored() throws Exception {
    URI uri = new URI("/foo");
    String version = HttpVersion.ONE_ONE;

    InputStream is = new ByteArrayInputStream(
          ("GET " + uri.toString() + " HTTP/" + version + " \r\n"
        ).getBytes(StandardCharsets.UTF_8));

    assertThat(Request.builder().buildWithStream(is),
               equalTo(Request.builder().withVersion(version).withMethod(Method.GET).withUri(uri).build()));
  }


  @Test
  void leadingWhitespaceCausesError() {
    InputStream is = new ByteArrayInputStream(" GET / HTTP/1.1\r\n".getBytes(StandardCharsets.UTF_8));
    assertThrows(RequestParsingException.class, () -> {
      Request.builder().buildWithStream(is);
    });
  }
}
