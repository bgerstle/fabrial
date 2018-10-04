package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.AuthorizationParsingException;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static com.eighthlight.fabrial.server.AdminCredentials.fromRequestHeaders;
import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;

public class AdminCredentialsFromHeaderTest {
  @Test
  void fromAuthHeaderEmptyIfFieldMissing() throws Exception {
    assertThat(fromRequestHeaders(Map.of()), equalTo(Optional.empty()));
  }

  @Test
  void fromAuthHeaderThrowsIfBasicPrefixMissing() {
    var authHeader = Map.of(
        "Authorization",
        "user:pass");
    AuthorizationParsingException result =
        (AuthorizationParsingException) Result.attempt(() -> fromRequestHeaders(authHeader))
                                              .getEither();
    assertThat(result, instanceOf(AuthorizationParsingException.class));
    assertThat(result.getMessage(), equalTo(AuthorizationParsingException.failedToMatch().getMessage()));
  }

  @Test
  void fromAuthHeaderThrowsIfCredentialsEmpty() {
    var authHeader = Map.of(
        "Authorization",
        "Basic ");
    AuthorizationParsingException result =
        (AuthorizationParsingException) Result.attempt(() -> fromRequestHeaders(authHeader))
                                              .getEither();
    assertThat(result, instanceOf(AuthorizationParsingException.class));
    assertThat(result.getMessage(), equalTo(AuthorizationParsingException.failedToMatch().getMessage()));
  }

  @Test
  void fromAuthHeaderThrowsIfCredentialsJustWhitespace() {
    var authHeader = Map.of(
        "Authorization",
        "Basic    ");
    var result = Result.attempt(() -> fromRequestHeaders(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException.malformedCredentials(null).getMessage()));
    assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  void fromAuthHeaderThrowsIfCredentialsMissingUser() {
    var authHeader = Map.of(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString(":password".getBytes()));
    var result = Result.attempt(() -> fromRequestHeaders(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException
                                           .malformedCredentials(null)
                                           .getMessage()));
    assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  void fromAuthHeaderThrowsIfCredentialsMissingPassword() {
    var authHeader = Map.of(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("user:".getBytes()));
    var result = Result.attempt(() -> fromRequestHeaders(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException
                                           .malformedCredentials(null)
                                           .getMessage()));
    assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
  }

  @Test
  void fromAuthHeaderThrowsIfCredentialsMissingColon() {
    var authHeader = Map.of(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("userpassword".getBytes()));

    var result = Result.attempt(() -> fromRequestHeaders(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(),
               equalTo(AuthorizationParsingException.malformedCredentials(null).getMessage()));
    assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
  }

  @Test
  void fromAuthHeaderThrowsIfDecodingFails() {
    // '!' is an illegal base64 character
    var authHeader = Map.of("Authorization", "Basic " + "!!!!!");
    var result = Result.attempt(() -> fromRequestHeaders(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException
                                           .malformedCredentials(null)
                                           .getMessage()));
    assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
    assertThat(e.getCause().getMessage(), containsString("Illegal base64 character"));
  }

  @Test
  void fromAuthHeaderAcceptsBase64EncodedUsernameAndPassword() {
    qt().forAll(alphanumeric(32),
                alphanumeric(32))
        .checkAssert((user, pass) -> {
          var userPass = user + ":" + pass;
          var encodedStr = Base64.getEncoder().encodeToString(userPass.getBytes());
          var authHeader = Map.of("Authorization", "Basic " + encodedStr);

          var creds = Result.attempt(() -> fromRequestHeaders(authHeader))
                            .orElseAssert();

          assertThat(creds.isPresent(), equalTo(true));
          assertThat(creds.get().username, equalTo(user));
          assertThat(creds.get().password, equalTo(pass));
        });
  }
}
