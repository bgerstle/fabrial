package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import org.quicktheories.core.Gen;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import static org.quicktheories.generators.Generate.*;
import static org.quicktheories.generators.SourceDSL.strings;

public class ArbitraryHttp {
  public static final Gen<URI> uris() {
    Gen<String> az = strings().betweenCodePoints('a', 'z').ofLengthBetween(0, 5);
    Gen<String> AZ = strings().betweenCodePoints('A', 'Z').ofLengthBetween(0, 5);
    Gen<String> nums = strings().betweenCodePoints('0', '9').ofLengthBetween(0, 5);
    Gen<String> special = pick(List.of("/", "-", "_"));
    Gen<String> path = az.mix(AZ).mix(nums).mix(special);
    return path.toOptionals(5).map(p -> {
      return p.flatMap(pp -> pp.startsWith("/") ? Optional.of(pp) : Optional.of("/" + pp))
              .orElse("/");
    })
    .map((str) -> {
        try {
          return new URI(str);
        } catch (URISyntaxException e) {
          throw new RuntimeException("Invalid syntax from generator", e);
        }
    });
  }

  public static final Gen<String> versions() {
    return pick(HttpVersion.allVersions);
  }

  public static final Gen<Method> methods() {
    return enumValues(Method.class);
  }
}
