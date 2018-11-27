package com.eighthlight.fabrial.test.http.request;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.http.message.request.RequestReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.headers;
import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
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
  void requestWithEmptyBody() throws Exception {
    var request = new RequestBuilder()
        .withVersion(HttpVersion.ONE_ONE)
        .withMethod(Method.PUT)
        .withUriString("/foo")
        .withBody(new ByteArrayInputStream(new byte[0]))
        .build();
    var requestReader = readerWithBytesFrom(request);
    var parsedRequest = requestReader.readRequest().get();

    assertThat(parsedRequest, equalTo(request));
    assertThat(parsedRequest.body.readAllBytes(), is(new byte[0]));
  }

  @Test
  void parsesPUTRequest() throws Exception {
    var body = "foo";
    var bodyData = body.getBytes(StandardCharsets.UTF_8);
    var bodyLength = bodyData.length;
    var request = new RequestBuilder()
        .withVersion(HttpVersion.ZERO_NINE)
        .withMethod(Method.GET)
        .withUriString("/")
        .withBody(new ByteArrayInputStream(bodyData))
        .build();
    var requestReader = readerWithBytesFrom(request);
    var parsedRequest = requestReader.readRequest().get();

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
          // reassemble request w/ headers & body
          final var request = new RequestBuilder()
              .withVersion(emptyRequest.version)
              .withMethodValue(emptyRequest.method)
              .withUri(emptyRequest.uri)
              .withBody(new ByteArrayInputStream(bodyData))
              .withHeaders(headersWithBodyFields)
              .build();

          var requestReader = readerWithBytesFrom(request);
          var parsedRequest = Result.attempt(requestReader::readRequest).orElseAssert().get();

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
