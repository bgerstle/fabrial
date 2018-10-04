package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.utils.Result;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class AdminCredentials {
  public final String username;
  public final String password;

  public static Optional<AdminCredentials> fromEnvironment(Map<String,String> env) {
    return Result
        .attempt(() -> {
          return Optional
              .ofNullable(env.get("ADMIN_USER"))
              .flatMap(user -> {
                return Optional.ofNullable(env.get("ADMIN_PASSWORD"))
                               .map(pw -> new AdminCredentials(user, pw));
              });
        })
        .orElseThrow();
  }

  public AdminCredentials(String username, String password) {
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("AdminCredentials username must be non-empty string");
    }
    if (password== null || password.isEmpty()) {
      throw new IllegalArgumentException("AdminCredentials password must be non-empty string");
    }
    this.username = username;
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AdminCredentials that = (AdminCredentials) o;
    return Objects.equals(username, that.username) &&
           Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password);
  }

  @Override
  public String toString() {
    return "AdminCredentials{*}";
  }
}
