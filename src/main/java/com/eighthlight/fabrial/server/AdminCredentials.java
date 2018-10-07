package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.utils.Result;

import java.util.Map;
import java.util.Optional;

public class AdminCredentials {
  public static final String USER_ENV_VAR = "ADMIN_USER";
  public static final String PASSWORD_ENV_VAR = "ADMIN_PASSWORD";

  public static Optional<Credential> fromEnvironment(Map<String,String> env) {
    return Result
        .attempt(() -> {
          return Optional
              .ofNullable(env.get(USER_ENV_VAR))
              .flatMap(user -> {
                return Optional.ofNullable(env.get(PASSWORD_ENV_VAR))
                               .map(pw -> new Credential(user, pw));
              });
        })
        .orElseThrow();
  }
}
