package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.AdminCredentials;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdminCredentialsTest {
  @Test
  void throwsWithInvalidArgs() {
    List.of(
        new String[]{null, "password"},
        new String[]{"", "password"},
        new String[]{"user", null},
        new String[]{"user", ""}
    ).forEach(creds -> {
      assertThrows(IllegalArgumentException.class, () -> new AdminCredentials(creds[0], creds[1]));
    });
  }

  @Test
  void constructsItselfFromEnvVars() {
    var credsMap = Map.of("ADMIN_USERNAME", "user", "ADMIN_PASSWORD", "password");
    var creds = AdminCredentials.fromEnvironment(credsMap);
    assertThat(creds.isPresent(), equalTo(true));
    assertThat(creds.get().username, equalTo(credsMap.get("ADMIN_USERNAME")));
    assertThat(creds.get().password, equalTo(credsMap.get("ADMIN_PASSWORD")));
  }

  @Test
  void returnsNullIfEitherEnvKeyIsMissing() {
    List.of(
        Map.of("ADMIN_USERNAME", "user"),
        Map.of("ADMIN_PASSWORD", "password")
    ).forEach(credsMap -> {
      assertThat(AdminCredentials.fromEnvironment(credsMap), equalTo(Optional.empty()));
    });
  }
}
