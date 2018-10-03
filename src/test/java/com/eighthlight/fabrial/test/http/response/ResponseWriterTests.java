package com.eighthlight.fabrial.test.http.response;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.http.message.response.ResponseWriter;
import com.eighthlight.fabrial.test.http.client.ResponseReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.maps;
import static org.quicktheories.generators.SourceDSL.strings;

public class ResponseWriterTests {
  private static Gen<ByteArrayInputStream> bodyStreams(int length) {
    return strings()
        .allPossible()
        .ofLengthBetween(0, length)
        .map(s -> {
          return Result.attempt(() -> {
            var chars = s.codePoints().toArray();
            var bytes = ByteBuffer.allocate(chars.length * 4);
            bytes.asIntBuffer().put(chars);
            return new ByteArrayInputStream(bytes.array());
          }).orElseAssert();
        });
  }

  private static Gen<Response> responses() {
    return httpVersions()
        .zip(statusCodes(),
             responseReasons(10).toOptionals(20),
             maps().of(alphanumeric(16), alphanumeric(16))
                   .ofSizeBetween(1, 5).toOptionals(50),
             bodyStreams(10).toOptionals(10),
             (version, statusCode, reason, headers, body) -> {
               return new Response(version,
                                   statusCode,
                                   reason.orElse(null),
                                   headers.orElse(null),
                                   body.orElse(new ByteArrayInputStream(new byte[0])));
             });
  }

  @Test
  void responseWithoutHeadersOrBody() throws Exception {
    var os = new ByteArrayOutputStream();
    var response = new ResponseBuilder()
        .withStatusCode(200)
        .withVersion(HttpVersion.ONE_ONE)
        .build();
    new ResponseWriter(os).writeResponse(response);
    var serializedResponseBytes = os.toByteArray();
    var is = new ByteArrayInputStream(serializedResponseBytes);
    var parsedResponse = new ResponseReader(is).read();
    assertThat(parsedResponse.isPresent(), is(true));
    assertThat(parsedResponse.get(), is(response));
    assertThat(parsedResponse.get().body.readAllBytes(), is(new byte[0]));
    os.close();
    is.close();
  }

  @Test
  void responseWithHeadersWithoutBody() throws Exception {
    var os = new ByteArrayOutputStream();
    var response = new ResponseBuilder()
        .withStatusCode(200)
        .withVersion(HttpVersion.ONE_ONE)
        .withHeader("Content-Length", "0")
        .build();
    new ResponseWriter(os).writeResponse(response);
    var serializedResponseBytes = os.toByteArray();
    var is = new ByteArrayInputStream(serializedResponseBytes);
    var parsedResponse = new ResponseReader(is).read();
    assertThat(parsedResponse.isPresent(), is(true));
    assertThat(parsedResponse.get(), is(response));
    assertThat(parsedResponse.get().body.readAllBytes(), is(new byte[0]));
    os.close();
    is.close();
  }

  @Test
  void responseWithHeadersAndBody() throws Exception {
    var os = new ByteArrayOutputStream();
    var response = new ResponseBuilder()
        .withStatusCode(200)
        .withVersion(HttpVersion.ONE_ONE)
        .withHeader("Content-Length", "3")
        .withBodyFromString("foo")
        .build();

    new ResponseWriter(os).writeResponse(response);
    var serializedResponseBytes = os.toByteArray();
    var is = new ByteArrayInputStream(serializedResponseBytes);
    var parsedResponse = new ResponseReader(is).read();
    assertThat(parsedResponse.isPresent(), is(true));
    assertThat(parsedResponse.get(), is(response));
    assertThat(new String(parsedResponse.get().body.readAllBytes()), is("foo"));

    os.close();
    is.close();
  }

  @Test
  void arbitraryResponseSerialization() {
    qt().forAll(responses()).checkAssert((response) -> {
      try (var os = new ByteArrayOutputStream()) {
        Result.attempt(() -> new ResponseWriter(os).writeResponse(response)).orElseAssert();
        os.flush();
        var serializedResponseBytes = os.toByteArray();
        try (var is = new ByteArrayInputStream(serializedResponseBytes)) {
          var parsedResponse = Result.attempt(new ResponseReader(is)::read).orElseAssert();
          assertThat(parsedResponse.isPresent(), is(true));
          assertThat(parsedResponse.get(), is(response));
          response.body.reset();
          var expectedBytes = response.body.readAllBytes();
          var actualBytes = parsedResponse.get().body.readAllBytes();
          assertThat(actualBytes, is(expectedBytes));
        }
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    });
  }
}
