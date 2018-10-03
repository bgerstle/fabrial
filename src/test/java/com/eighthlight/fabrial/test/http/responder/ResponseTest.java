package com.eighthlight.fabrial.test.http.responder;

import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseBuilder;
import org.junit.jupiter.api.Test;
import org.quicktheories.api.Tuple4;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.oneOf;
import static org.quicktheories.generators.Generate.pick;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.strings;

public class ResponseTest {
  static Gen<Integer> invalidStatuses() {
    return oneOf(integers().between(Integer.MIN_VALUE, 99),
                 integers().between(1000, Integer.MAX_VALUE));
  }

  // characters outside ascii code points
  static Gen<String> invalidReasons() {
    return nonAsciiStrings();
  }

  private static ResponseBuilder builderWithFields(Tuple4<Integer, String, Optional<String>, Optional<Map<String, String>>> fields) {
    var builder = new ResponseBuilder().withStatusCode(fields._1)
                                .withVersion(fields._2);
    fields._3.ifPresent(builder::withReason);
    fields._4.ifPresent(builder::withHeaders);
    return builder;
  }

  static Gen<Tuple4<Integer, String, Optional<String>, Optional<Map<String, String>>>>
  responseFields() {
    return statusCodes()
        .zip(httpVersions(),
             responseReasons(32).toOptionals(30),
             headers().toOptionals(30),
             Tuple4::of);
  }

  @Test
  void constructsWithValidInput() {
    qt().forAll(responseFields())
        .checkAssert((fields) -> {
          var builder = builderWithFields(fields);
          Response resp = builder.build();
          assertThat(resp.statusCode, equalTo(fields._1));
          assertThat(resp.version, equalTo(fields._2));
          assertThat(resp.reason, equalTo(fields._3.orElse(null)));
          assertThat(resp.headers, equalTo(fields._4.orElse(Map.of())));
        });
  }

  @Test
  void responseEquality() {
    qt().forAll(responseFields())
        .checkAssert((fields) -> {
          var builder = builderWithFields(fields);
          var r1 = builder.build();
          var r2 = builder.build();
          assertThat(r1, equalTo(r2));
          assertThat(r1.hashCode(), equalTo(r2.hashCode()));
        });
  }

  @Test
  void responseEqualityIgnoresBody() {
    var optBodyGen = strings().allPossible()
                              .ofLength(3)
                              .toOptionals(50)
                              .map(os -> os.map(String::getBytes)
                                           .map(ByteArrayInputStream::new));
    qt().forAll(responseFields(), optBodyGen, optBodyGen)
        .checkAssert((fields, b1, b2) -> {
          var respBuilder1 = builderWithFields(fields);
          b1.ifPresent(respBuilder1::withBody);
          var respBuilder2 = builderWithFields(fields);
          b2.ifPresent(respBuilder2::withBody);
          var r1 = respBuilder1.build();
          var r2 = respBuilder2.build();
          assertThat(r1, equalTo(r2));
          assertThat(r1.hashCode(), equalTo(r2.hashCode()));
        });
  }

  @Test
  void responseInequality() {
    qt().forAll(responseFields(), responseFields())
        .assuming((f1, f2) -> !f1.equals(f2))
        .checkAssert((f1, f2) -> {
          var r1 = builderWithFields(f1).build();
          var r2 = builderWithFields(f2).build();
          assertThat(r1, not(equalTo(r2)));
          assertThat(r1.hashCode(), not(equalTo(r2.hashCode())));
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
