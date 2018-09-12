package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Response;
import com.eighthlight.fabrial.http.ResponseBuilder;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.util.List;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.oneOf;
import static org.quicktheories.generators.Generate.pick;
import static org.quicktheories.generators.SourceDSL.integers;

public class HttpResponseTesting {
  static Gen<Integer> invalidStatuses() {
    return oneOf(integers().between(Integer.MIN_VALUE, 99),
                 integers().between(1000, Integer.MAX_VALUE));
  }

  // characters outside ascii code points
  static Gen<String> invalidReasons() {
    return nonAsciiStrings();
  }


  @Test
  void constructsWithValidInput() {
    qt().forAll(httpVersions(), statusCodes(), responseReasons(32).toOptionals(30))
        .checkAssert((v, s, r) -> {
          var builder = new ResponseBuilder().withVersion(v).withStatusCode(s);
          builder = r.map(builder::withReason).orElse(builder);
          Response resp = builder.build();
          assertThat(resp.version, equalTo(v));
          assertThat(resp.statusCode, equalTo(s));
          assertThat(resp.reason, equalTo(r.orElse(null)));
        });
  }

  @Test
  void throwsWhenGivenInvalidVersion() {
    qt().forAll(pick(List.of("0.8", "2.6", "0.0")),
                invalidStatuses(),
                responseReasons(32).toOptionals(30))
        .checkAssert((v, s, r) ->
                         assertThrows(IllegalArgumentException.class, () ->
                             new ResponseBuilder()
                                 .withVersion(v)
                                 .withStatusCode(s)
                                 .withReason(r.orElse(null))
                                 .build()));
  }

  @Test
  void throwsWhenGivenInvalidStatusCode() {
    qt().forAll(httpVersions(), invalidStatuses(), responseReasons(32).toOptionals(30))
        .checkAssert((v, s, r) ->
                         assertThrows(IllegalArgumentException.class, () ->
                             new ResponseBuilder().withVersion(v).withStatusCode(s).build()));
  }

  @Test
  void throwsWhenGivenInvalidReason() {
    qt().forAll(httpVersions(), statusCodes(), invalidReasons())
        .checkAssert((v, s, r) ->
                         assertThrows(IllegalArgumentException.class, () ->
                             new ResponseBuilder()
                                 .withVersion(v)
                                 .withStatusCode(s)
                                 .withReason(r)
                                 .build()));
  }
}
