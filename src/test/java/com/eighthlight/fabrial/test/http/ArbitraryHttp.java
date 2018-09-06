package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import org.quicktheories.core.Gen;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static org.quicktheories.generators.Generate.*;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.lists;
import static org.quicktheories.generators.SourceDSL.strings;

public class ArbitraryHttp {
  static Gen<String> htab() {
    return constant(String.valueOf(0X2B7E));
  }

  // !!!: all of these "length" args are rough approximations. proper DSL necessary
  public static Gen<String> responseReasons(int length) {
    return strings().ascii().ofLengthBetween(1, length).mix(htab());
  }

  public static Gen<String> nonAsciiStrings() {
    final int LAST_ASCII_CODE_POINT = 0x007F;
    return strings().betweenCodePoints(LAST_ASCII_CODE_POINT + 1, Character.MAX_CODE_POINT)
                    .ofLengthBetween(1, 32);
  }

  // Characters which can be included in URI paths without percent encoding
  public static Gen<String> unreservedCharacters(int length) {
    return lists().of(pick(List.of("~", ".", "-", "_")))
                  .ofSize(length)
                  .map(ss -> ss.stream().reduce(String::concat).get());
  }

  public static Gen<Integer> statusCodes() {
    return integers().between(100, 999);
  }

  // Currently doesn't include optional port specifications or IP addresses
  public static Gen<String> hosts(int length) {
    return alphanumeric(length/2)
        .zip(constant("-").toOptionals(10),
        alphanumeric(length/2),
        (prefix, optDash, suffix) ->
          prefix + optDash.orElse("") + suffix
        );
  }

  // Doesn't include percent-encoded reserved characters
  public static Gen<String> paths(int length) {
    Gen<String> root = constant("/");
    Gen<Optional<String>> firstComponent = alphanumeric(length/2).toOptionals(10);
    // hard-coding mix of unreserved & alphanumeric characters
    Gen<Optional<String>> rest =
        alphanumeric(length/2 - 3)
        .mix(unreservedCharacters(3))
        .mix(constant("/"))
        .toOptionals(50);
    return root.zip(firstComponent, rest, (r, optFc, optRest) ->
      r
      + optFc.orElse("")
      // only include "rest" of the path if first component is present
      + optRest.flatMap(res -> optFc.isPresent() ? Optional.of(res) : Optional.empty()).orElse("")
    );
  }

  // Just covering HTTP for now
  public static Gen<String> schemes() {
    return oneOf(constant("http"), constant("https"));
  }

  public static final Gen<URI> requestTargets() {
    return schemes().toOptionals(10).zip(hosts(10),
                         paths(10).toOptionals(20),
                         (scheme, host, optPath) -> {
      try {
        return new URI(scheme.isPresent() ? scheme.get() : null,
                       scheme.isPresent() ? host : null,
                       optPath.orElse("/"),
                       null);
      } catch (URISyntaxException e) {
        throw new RuntimeException("origin URI failed to generate valid URI", e);
      }
    });
  }

  public static final Gen<String> httpVersions() {
    return pick(HttpVersion.allVersions);
  }

  public static final Gen<Method> methods() {
    return enumValues(Method.class);
  }
}
