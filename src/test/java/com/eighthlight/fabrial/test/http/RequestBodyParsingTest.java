package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.http.request.RequestReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.headers;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

public class RequestBodyParsingTest {
  private static RequestReader readerWithBytesFrom(Request request) {
    var requestOS = new ByteArrayOutputStream();
    Result.attempt(() -> new RequestWriter(requestOS).writeRequest(request))
          .orElseAssert();
    return new RequestReader(new ByteArrayInputStream(requestOS.toByteArray()));
  }

  @Test
  void omitsBodyWhenContentLengthMissing() throws Exception {
    var body = "foo";
    var request = new RequestBuilder()
        .withVersion(HttpVersion.ONE_ONE)
        .withMethod(Method.PUT)
        .withUriString("/foo")
        .withBody(new ByteArrayInputStream(body.getBytes()))
        .build();
    var requestReader = readerWithBytesFrom(request);
    var parsedRequest = requestReader.readRequest();

    assertThat(parsedRequest, equalTo(request));
    assertThat(parsedRequest.body, is(nullValue()));;
  }

  @Test
  void parsesPUTRequest() throws Exception {
    var body = "foo";
    var bodyData = body.getBytes(StandardCharsets.UTF_8);
    var bodyLength = bodyData.length;
    var request = new RequestBuilder()
        .withVersion(HttpVersion.ZERO_NINE)
        .withMethod(Method.GET)
        .withHeaders(Map.of(
            "Content-Type", "text/plain",
            "Content-Length", Integer.toString(bodyLength)
        ))
        .withUriString("/")
        .withBody(new ByteArrayInputStream(bodyData))
        .build();
    var requestReader = readerWithBytesFrom(request);
    var parsedRequest = requestReader.readRequest();

    assertThat(parsedRequest, equalTo(request));
    assertThat(parsedRequest.body, notNullValue());
    var parsedRequestBody = new String(parsedRequest.body.readAllBytes(), StandardCharsets.UTF_8);
    assertThat(parsedRequestBody, equalTo(body));
  }

  @Test
  void parsesRequestWithHeaderAndBody() {
    qt().forAll(requests(), headers(), strings().allPossible().ofLengthBetween(1, 32))
        .checkAssert((emptyRequest, headers, body) -> {
          var headersWithBodyFields = new HashMap<>(headers);
          var bodyChars = body.codePoints().toArray();
          var bodyByteBuffer = ByteBuffer.allocate(bodyChars.length * 4);
          bodyByteBuffer.asIntBuffer().put(bodyChars);
          var bodyData = bodyByteBuffer.array();
          headersWithBodyFields.putAll(Map.of(
              "Content-Type", "text/plain",
              "Content-Length", Integer.toString(bodyData.length)
          ));
          // reassemble request w/ headers & body
          final var request = new RequestBuilder()
              .withVersion(emptyRequest.version)
              .withMethod(emptyRequest.method)
              .withUri(emptyRequest.uri)
              .withBody(new ByteArrayInputStream(bodyData))
              .withHeaders(headersWithBodyFields)
              .build();

          var requestReader = readerWithBytesFrom(request);
          var parsedRequest = Result.attempt(requestReader::readRequest).orElseAssert();

          assertThat(parsedRequest, equalTo(request));
          assertThat(parsedRequest.body, notNullValue());

          var parsedBodyData =
              Result.attempt(parsedRequest.body::readAllBytes)
                    .map(ByteBuffer::wrap)
                    .orElseAssert();
          var intBuf = new int[bodyChars.length];
          parsedBodyData.asIntBuffer().get(intBuf, 0 , bodyChars.length);
          var parsedBody = new String(intBuf, 0 , intBuf.length);
          assertThat(parsedBody,  equalTo(body));
        });
  }
}
