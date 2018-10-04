package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.AccessLogResponder;
import com.eighthlight.fabrial.http.AccessLogger;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasKey;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.lists;

public class AccessLogResponderTest {
  @Test
  void respondsWithEmptyBodyWhenLogIsEmpty() throws IOException {
    var logger = new AccessLogger();
    var responder = new AccessLogResponder(logger);

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.GET)
            .withUriString("/logs")
            .build());

    assertThat(response.statusCode, equalTo(200));
    assertThat(response.body.readAllBytes(), equalTo(new byte[0]));
  }

  @ParameterizedTest
  @ValueSource(strings = {"PUT", "POST", "DELETE"})
  void responds405ToDisallowedMethods(String method) {
    var logger = new AccessLogger();
    var responder = new AccessLogResponder(logger);
    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.valueOf(method))
            .withUriString("/logs")
            .build());
    assertThat(response.statusCode, equalTo(405));
  }

  @Test
  void respondsToHead() {
    var logger = new AccessLogger();
    var responder = new AccessLogResponder(logger);
    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString("/logs")
            .build());
    assertThat(response.statusCode, equalTo(200));
  }

  @Test
  void respondsToOptions() {
    var logger = new AccessLogger();
    var responder = new AccessLogResponder(logger);
    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.OPTIONS)
            .withUriString("/logs")
            .build());
    assertThat(response.statusCode, equalTo(200));
    assertThat(response.headers, hasKey("Allow"));
    var allowHeaderField = response.headers.get("Allow");
    var allowedMethods =
        Arrays.stream(allowHeaderField.split(", *"))
        .map(Method::valueOf)
        .collect(Collectors.toList());
    assertThat(allowedMethods, containsInAnyOrder(Method.GET, Method.HEAD, Method.OPTIONS));
  }

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
