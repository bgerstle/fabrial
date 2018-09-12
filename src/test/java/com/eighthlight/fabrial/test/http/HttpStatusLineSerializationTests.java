package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Response;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;

public class HttpStatusLineSerializationTests {
  @Test
  void responseWithoutHeadersOrBody() {
    qt()
        .forAll(httpVersions(),
                statusCodes(),
                responseReasons(32).toOptionals(30))
        .checkAssert((version, status, optReason) -> {
          Response resp = new Response(version,
                                       status,
                                       optReason.orElse(null));
          String line = serializeResponse(resp);
          assertThat(line,
                     equalTo("HTTP/" + version + " "
                             + Integer.toString(status) + " "
                             + optReason.orElse("")
                             + CRLF));
        });
  }

  private String serializeResponse(Response resp) throws RuntimeException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try {
      resp.writeTo(os);
      return new String(os.toByteArray(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void successfulResponse() throws Exception {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    new Response("1.1", 200, null).writeTo(os);
    String line = new String(os.toByteArray(), StandardCharsets.UTF_8);
    assertThat(line,
               equalTo("HTTP/1.1 200 " + CRLF));
  }
}
