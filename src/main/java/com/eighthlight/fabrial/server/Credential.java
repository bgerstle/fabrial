package com.eighthlight.fabrial.server;

import java.util.Objects;

public class Credential {
  public final String username;
  public final String password;

  public Credential(String username, String password) {
    if (username == null || username.isEmpty()) {
      throw new IllegalArgumentException("Credential username must be non-empty string");
    }
    if (password== null || password.isEmpty()) {
      throw new IllegalArgumentException("Credential password must be non-empty string");
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
    Credential that = (Credential) o;
    return Objects.equals(username, that.username) &&
           Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, password);
  }

  @Override
  public String toString() {
    return "Credential{*}";
  }
}
