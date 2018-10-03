package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.AccessLogResponder;
import com.eighthlight.fabrial.http.AccessLogger;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.lists;

public class AccessLogResponderTest {
  @Test
  void respondsWithBodyContainingFormattedLog() {
    qt().forAll(lists().of(requests()).ofSizeBetween(1, 100))
        .checkAssert(rs -> {
          var logger = new AccessLogger();
          var responder = new AccessLogResponder(logger);
          rs.forEach(logger::log);

          var response = responder.getResponse(
              new RequestBuilder()
                  .withVersion(HttpVersion.ONE_ONE)
                  .withMethod(Method.GET)
                  .withUriString("/logs")
                  .build());

          assertThat(response.statusCode, equalTo(200));

          var actualBodyLines = Arrays.asList(
              new String(Result.attempt(response.body::readAllBytes).orElseAssert()).split("\n"));
          var expectedBodyLines =
              rs.stream()
                .map(r -> String.join(" ",
                                      r.method.name(),
                                      r.uri.getPath().toString(),
                                      r.version))
                .collect(Collectors.toList());

          assertThat(actualBodyLines, equalTo(expectedBodyLines));
        });
  }
}
