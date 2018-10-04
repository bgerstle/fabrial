package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.utils.Result;

import java.util.Map;
import java.util.Optional;

public class AdminCredentials {
  public static Optional<Credential> fromEnvironment(Map<String,String> env) {
    return Result
        .attempt(() -> {
          return Optional
              .ofNullable(env.get("ADMIN_USER"))
              .flatMap(user -> {
                return Optional.ofNullable(env.get("ADMIN_PASSWORD"))
                               .map(pw -> new Credential(user, pw));
              });
        })
        .orElseThrow();
  }
}
