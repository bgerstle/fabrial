package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.Credential;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CredentialTest {
  @Test
  void throwsWithInvalidArgs() {
    List.of(
        new String[]{null, "password"},
        new String[]{"", "password"},
        new String[]{"user", null},
        new String[]{"user", ""}
    ).forEach(creds -> {
      assertThrows(IllegalArgumentException.class, () -> new Credential(creds[0], creds[1]));
    });
  }
}
