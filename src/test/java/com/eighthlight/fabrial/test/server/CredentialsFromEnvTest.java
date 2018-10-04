package com.eighthlight.fabrial.test.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.eighthlight.fabrial.server.AdminCredentials.fromEnvironment;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CredentialsFromEnvTest {
  @Test
  void buildableFromEnvVars() {
    var credsMap = Map.of("ADMIN_USERNAME", "user", "ADMIN_PASSWORD", "password");
    var creds = fromEnvironment(credsMap);
    assertThat(creds.isPresent(), equalTo(true));
    assertThat(creds.get().username, equalTo(credsMap.get("ADMIN_USERNAME")));
    assertThat(creds.get().password, equalTo(credsMap.get("ADMIN_PASSWORD")));
  }

  @Test
  void fromEnvIsEmptyIfEitherVarIsMissing() {
    List.of(
        Map.of("ADMIN_USERNAME", "user"),
        Map.of("ADMIN_PASSWORD", "password")
    ).forEach(credsMap -> {
      assertThat(fromEnvironment(credsMap), equalTo(Optional.empty()));
    });
  }
}
