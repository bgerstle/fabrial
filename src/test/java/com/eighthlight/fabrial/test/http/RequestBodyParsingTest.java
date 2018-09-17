package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.http.request.RequestReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.headers;
import static com.eighthlight.fabrial.test.http.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

public class RequestBodyParsingTest {
  @Test
  void parsesRequestWithHeader() {
    qt().forAll(requests(), headers(), strings().allPossible().ofLengthBetween(1, 32))
        .checkAssert((request, headers, body) -> {
          var requestLine = HttpRequestLineParsingTests.concatRequestLineComponents(
              request.method.name(),
              request.uri.toString(),
              request.version);
          var headerLines =
              HttpHeaderReaderTest.headerLineFromComponents(headers, "","");
          var requestMessage = requestLine + headerLines + body;
          var requestReader = new RequestReader(new ByteArrayInputStream(requestMessage.getBytes()));

          var parsedRequest = Result.attempt(requestReader::readRequest).orElseAssert();

          assertThat(parsedRequest,
                     equalTo(new RequestBuilder()
                                 .withVersion(request.version)
                                 .withMethod(request.method)
                                 .withUri(request.uri)
                                 .withHeaders(headers)
                                 .build()));

          assertThat(parsedRequest.body, notNullValue());

          var baos = new ByteArrayOutputStream();
          Result.attempt(() -> parsedRequest.body.transferTo(baos)).orElseAssert();
          var requestBody = baos.toString(StandardCharsets.UTF_8);
          
          assertThat(requestBody, equalTo(body));
        });
  }
}
