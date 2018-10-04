package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.AdminCredential;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AdminCredentialTest {
  @Test
  void throwsWithInvalidArgs() {
    List.of(
        new String[]{null, "password"},
        new String[]{"", "password"},
        new String[]{"user", null},
        new String[]{"user", ""}
    ).forEach(creds -> {
      assertThrows(IllegalArgumentException.class, () -> new AdminCredential(creds[0], creds[1]));
    });
  }
}
