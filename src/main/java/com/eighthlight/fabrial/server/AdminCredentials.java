package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.utils.Result;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class AdminCredentials {
  public static Optional<AdminCredential> fromEnvironment(Map<String,String> env) {
    return Result
        .attempt(() -> {
          return Optional
              .ofNullable(env.get("ADMIN_USER"))
              .flatMap(user -> {
                return Optional.ofNullable(env.get("ADMIN_PASSWORD"))
                               .map(pw -> new AdminCredential(user, pw));
              });
        })
        .orElseThrow();
  }

  private static final Pattern credentialPattern = Pattern.compile("^Basic (.+)$");

  public static Optional<AdminCredential> fromRequestHeaders(Map<String, String> headers) throws AuthorizationParsingException {
    var headerValue =  headers.get("Authorization");
    if (headerValue == null) {
      return Optional.empty();
    }

    var m = credentialPattern.matcher(headerValue);
    if (!m.matches()) {
      throw AuthorizationParsingException.failedToMatch();
    }

    var encodedCredentials = m.group(1);
    if (encodedCredentials == null || encodedCredentials.isEmpty()) {
      throw AuthorizationParsingException.emptyCredentials();
    }

    try {
      var decodedCreds = new String(Base64.getDecoder().decode(encodedCredentials));
      var credComponents = decodedCreds.split(":");
      return Optional.of(new AdminCredential(credComponents[0], credComponents[1]));
    } catch (Throwable t) {
      throw AuthorizationParsingException.malformedCredentials(t);
    }
  }
}
