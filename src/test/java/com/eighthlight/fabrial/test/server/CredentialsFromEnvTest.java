package com.eighthlight.fabrial.test.server;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.eighthlight.fabrial.server.AdminCredentials.PASSWORD_ENV_VAR;
import static com.eighthlight.fabrial.server.AdminCredentials.USER_ENV_VAR;
import static com.eighthlight.fabrial.server.AdminCredentials.fromEnvironment;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CredentialsFromEnvTest {
  @Test
  void buildableFromEnvVars() {
    var credsMap = Map.of(USER_ENV_VAR, "user", PASSWORD_ENV_VAR, "password");
    var creds = fromEnvironment(credsMap);
    assertThat(creds.isPresent(), equalTo(true));
    assertThat(creds.get().username, equalTo(credsMap.get(USER_ENV_VAR)));
    assertThat(creds.get().password, equalTo(credsMap.get(PASSWORD_ENV_VAR)));
  }

  @Test
  void fromEnvIsEmptyIfEitherVarIsMissing() {
    List.of(
        Map.of(USER_ENV_VAR, "user"),
        Map.of(PASSWORD_ENV_VAR, "password")
    ).forEach(credsMap -> {
      assertThat(fromEnvironment(credsMap), equalTo(Optional.empty()));
    });
  }
}
