package com.eighthlight.fabrial.test.http.request;

import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.http.message.request.RequestReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.headers;
import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;

public class RequestHeaderParsingTest {
  @Test
  void parsesRequestWithHeader() {
    qt().forAll(requests(), headers())
        .as((req, headers) -> {
          return new RequestBuilder()
              .withVersion(req.version)
              .withMethodValue(req.method)
              .withUri(req.uri)
              .withHeaders(headers)
              .build();
        })
        .checkAssert((request) -> {
          var requestLine = HttpRequestLineParsingTests.concatRequestLineComponents(
              request.method,
              request.uri.toString(),
              request.version);
          var headerLines =
              HttpHeaderReaderTest.headerLineFromComponents(request.headers, "", "");
          var requestMessage = requestLine + headerLines;
          var requestReader = new RequestReader(new ByteArrayInputStream(requestMessage.getBytes()));
          assertThat(Result.attempt(requestReader::readRequest).orElseAssert().get(),
                     equalTo(request));
        });
  }
}
