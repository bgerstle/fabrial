package com.eighthlight.fabrial.http.auth;

import com.eighthlight.fabrial.server.Credential;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class BasicAuth {
  private static final String prefix = "Basic";
  private static final String delimiter = ":";
  private static final Pattern credentialPattern = Pattern.compile("^" + prefix + " (.+)$");

  public static Optional<Credential> decode(Map<String, String> headers) throws AuthorizationParsingException {
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
      var credComponents = decodedCreds.split(delimiter);
      return Optional.of(new Credential(credComponents[0], credComponents[1]));
    } catch (Throwable t) {
      throw AuthorizationParsingException.malformedCredentials(t);
    }
  }

  public static Map<String, String> encode(Credential credential) {
    Objects.requireNonNull(credential);
    return Map.of("Authorization",
                  prefix
                  + " "
                  + Base64.getEncoder().encodeToString((credential.username
                                                        + ":"
                                                        + credential.password).getBytes()));
  }
}
