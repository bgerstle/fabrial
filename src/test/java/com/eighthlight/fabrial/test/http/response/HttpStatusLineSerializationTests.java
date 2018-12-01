package com.eighthlight.fabrial.test.http.response;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.message.response.HttpStatusLineWriter;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.bgerstle.result.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;

public class HttpStatusLineSerializationTests {
  private static String getStatusLineForResponse(Response resp) {
    return Result.attempt(() -> {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      try {
        new HttpStatusLineWriter(os).writeStatusLine(resp.version, resp.statusCode, resp.reason);
        return new String(os.toByteArray(), StandardCharsets.UTF_8);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }).orElseAssert();
  }

  @Test
  void responseWithoutHeadersOrBody() {
    qt()
        .forAll(httpVersions(),
                statusCodes(),
                responseReasons(32).toOptionals(30))
        .checkAssert((version, status, optReason) -> {
          String line = getStatusLineForResponse(
              new ResponseBuilder()
                  .withVersion(version)
                  .withStatusCode(status)
                  .withReason(optReason.orElse(null))
                  .build());
          assertThat(line,
                     equalTo("HTTP/" + version + " "
                             + Integer.toString(status) + " "
                             + optReason.orElse("")
                             + CRLF));
        });
  }

  @Test
  void successfulResponse() {
    String line = getStatusLineForResponse(new ResponseBuilder()
                                               .withVersion(HttpVersion.ONE_ONE)
                                               .withStatusCode(200)
                                               .build());
    assertThat(line,
               equalTo("HTTP/1.1 200 " + CRLF));
  }
}
