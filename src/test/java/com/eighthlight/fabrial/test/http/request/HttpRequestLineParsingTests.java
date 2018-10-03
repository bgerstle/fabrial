package com.eighthlight.fabrial.test.http.request;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.http.request.RequestParsingException;
import com.eighthlight.fabrial.http.request.RequestReader;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.*;

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

  public static String concatRequestLineComponents(
      String method,
      String uri,
      String version) {
    return method + " "
           + uri + " "
           + "HTTP/" + version
           + CRLF;
  }

  public static ByteArrayInputStream requestLineFromComponents(
      String method,
      String uri,
      String version) {
    return new ByteArrayInputStream(concatRequestLineComponents(method, uri, version).getBytes());
  }

  @Test
  void requestsWithoutHeadersOrBody() {
    qt().forAll(methods(), requestTargets(), httpVersions())
        .checkAssert((m, u, v) -> {
          Object req;
          try {
            req = new RequestReader(requestLineFromComponents(m.name(), u.toString(), v)).readRequest().get();
          } catch (RequestParsingException e) {
            req = e;
          }
          assertThat(req,
                     equalTo(new RequestBuilder().withVersion(v).withMethod(m).withUri(u).build()));
        });
  }

  @Test
  void throwsWithMissingComponents() {
    qt().forAll(methods().toOptionals(75),
                requestTargets().toOptionals(75),
                httpVersions().toOptionals(75))
        .assuming((m, u , v) ->
                      !(m.isPresent() && u.isPresent() && v.isPresent())
        )
        .checkAssert((method, uri, version) -> {
          ByteArrayInputStream is =
              requestLineFromComponents(
                  method.map(Method::name).orElse(""),
                  uri.map(URI::toString).orElse(""),
                  version.orElse("")
              );

          assertThrows(RequestParsingException.class, () -> {
            new RequestReader(is).readRequest().get();
          });
        });
  }

  @Test
  void throwsWithBadTargets() {
    qt().forAll(invalidMethods(), invalidUris(), httpVersions())
        .checkAssert((m, u, v) -> {
          assertThrows(RequestParsingException.class, () -> {
            new RequestReader(requestLineFromComponents(m, u, v)).readRequest().get();
          });
        });
  }

  @Test
  void throwsWithBadVersions() {
    qt().forAll(methods(), requestTargets(), invalidVersions())
        .checkAssert((m, u, v) -> {
          assertThrows(RequestParsingException.class, () -> {
            new RequestReader(requestLineFromComponents(m.name(), u.toString(), v)).readRequest().get();
          });
        });
  }

  @Test
  void trailingWhitespaceIsIgnored() throws Exception {
    URI uri = new URI("/foo");
    String version = HttpVersion.ONE_ONE;

    InputStream is = new ByteArrayInputStream(
          ("GET " + uri.toString() + " HTTP/" + version + " " + CRLF
        ).getBytes(StandardCharsets.UTF_8));

    assertThat(new RequestReader(is).readRequest().get(),
               equalTo(new RequestBuilder().withVersion(version).withMethod(Method.GET).withUri(uri).build()));
  }


  @Test
  void leadingWhitespaceCausesError() throws Exception {
    InputStream is = new ByteArrayInputStream((" GET / HTTP/1.1" + CRLF).getBytes(StandardCharsets.UTF_8));
    assertThrows(RequestParsingException.class, () -> {
      new RequestReader(is).readRequest().get();
    });
  }

  @Test
  void emptyStringTest() throws Exception {
    assertThat(new RequestReader(new ByteArrayInputStream(new byte[0])).readRequest(),
               equalTo(Optional.empty()));
  }
}
