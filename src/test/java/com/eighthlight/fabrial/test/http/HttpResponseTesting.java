package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Response;
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
import static org.quicktheories.generators.SourceDSL.strings;

public class HttpResponseTesting {
  static Gen<Integer> invalidStatuses() {
    return oneOf(integers().between(Integer.MIN_VALUE, 99),
                 integers().between(1000, Integer.MAX_VALUE));
  }

  // characters outside ascii code points
  static Gen<String> invalidReasons() {
    final int LAST_ASCII_CODE_POINT = 0x007F;
    return strings().betweenCodePoints(LAST_ASCII_CODE_POINT + 1, Character.MAX_CODE_POINT)
                    .ofLengthBetween(1, 32);
  }


  @Test
  void constructsWithValidInput() {
    qt().forAll(httpVersions(), statusCodes(), responseReasons(32).toOptionals(30))
        .checkAssert((v, s, r) -> {
          Response resp = new Response(v, s, r.orElse(null));
          assertThat(resp.version, equalTo(v));
          assertThat(resp.statusCode, equalTo(s));
          assertThat(resp.reason, equalTo(r));
        });
  }

  @Test
  void throwsWhenGivenInvalidVersion() {
    qt().forAll(pick(List.of("0.8", "2.6", "0.0")), invalidStatuses(), responseReasons(32).toOptionals(30))
        .checkAssert((v, s, r) ->
                         assertThrows(IllegalArgumentException.class, () ->
                             new Response(v, s,r.orElse(null))
                         )
        );
  }

  @Test
  void throwsWhenGivenInvalidStatusCode() {
    qt().forAll(httpVersions(), invalidStatuses(), responseReasons(32).toOptionals(30))
        .checkAssert((v, s, r) ->
                         assertThrows(IllegalArgumentException.class, () ->
                             new Response(v, s,null)
                         )
        );
  }

  @Test
  void throwsWhenGivenInvalidReason() {
    qt().forAll(httpVersions(), statusCodes(), invalidReasons())
        .checkAssert((v, s, r) ->
                         assertThrows(IllegalArgumentException.class, () ->
                             new Response(v, s, r)
                         )
        );
  }
}
