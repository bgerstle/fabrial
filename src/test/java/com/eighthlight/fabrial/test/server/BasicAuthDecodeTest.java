package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.auth.AuthorizationParsingException;
import com.eighthlight.fabrial.http.auth.BasicAuth;
import com.eighthlight.fabrial.server.Credential;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static com.eighthlight.fabrial.test.gen.ArbitraryStrings.alphanumeric;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;

public class BasicAuthDecodeTest {
  @Test
  void decodeEmptyIfFieldMissing() throws Exception {
    assertThat(BasicAuth.decode(Map.of()), equalTo(Optional.empty()));
  }

  @Test
  void decodeThrowsIfBasicPrefixMissing() {
    var authHeader = Map.of(
        "Authorization",
        "user:pass");
    AuthorizationParsingException result =
        (AuthorizationParsingException) Result.attempt(() -> BasicAuth.decode(authHeader))
                                              .getEither();
    assertThat(result, instanceOf(AuthorizationParsingException.class));
    assertThat(result.getMessage(), equalTo(AuthorizationParsingException.failedToMatch().getMessage()));
  }

  @Test
  void decodeThrowsIfCredentialsEmpty() {
    var authHeader = Map.of(
        "Authorization",
        "Basic ");
    AuthorizationParsingException result =
        (AuthorizationParsingException) Result.attempt(() -> BasicAuth.decode(authHeader))
                                              .getEither();
    assertThat(result, instanceOf(AuthorizationParsingException.class));
    assertThat(result.getMessage(), equalTo(AuthorizationParsingException.failedToMatch().getMessage()));
  }

  @Test
  void decodeThrowsIfCredentialsJustWhitespace() {
    var authHeader = Map.of(
        "Authorization",
        "Basic    ");
    var result = Result.attempt(() -> BasicAuth.decode(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException.malformedCredentials(null).getMessage()));
    assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  void decodeThrowsIfCredentialsMissingUser() {
    var authHeader = Map.of(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString(":password".getBytes()));
    var result = Result.attempt(() -> BasicAuth.decode(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException
                                           .malformedCredentials(null)
                                           .getMessage()));
    assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));
  }

  @Test
  void decodeThrowsIfCredentialsMissingPassword() {
    var authHeader = Map.of(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("user:".getBytes()));
    var result = Result.attempt(() -> BasicAuth.decode(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(), equalTo(AuthorizationParsingException
                                           .malformedCredentials(null)
                                           .getMessage()));
    assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
  }

  @Test
  void decodeThrowsIfCredentialsMissingColon() {
    var authHeader = Map.of(
        "Authorization",
        "Basic " + Base64.getEncoder().encodeToString("userpassword".getBytes()));

    var result = Result.attempt(() -> BasicAuth.decode(authHeader));
    assertThat(result.getError().isPresent(), is(true));
    Exception e = result.getError().get();
    assertThat(e, instanceOf(AuthorizationParsingException.class));
    assertThat(e.getMessage(),
               equalTo(AuthorizationParsingException.malformedCredentials(null).getMessage()));
    assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
  }

  @Test
  void decodeThrowsIfDecodingFails() {
    // '!' is an illegal base64 character
    var authHeader = Map.of("Authorization", "Basic " + "!!!!!");
    var result = Result.attempt(() -> BasicAuth.decode(authHeader));
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
  void decodeAcceptsBase64EncodedUsernameAndPassword() {
    qt().forAll(alphanumeric(32),
                alphanumeric(32))
        .checkAssert((user, pass) -> {
          var expectedCredential = new Credential(user, pass);
          var actualCredential =
              Result.attempt(() -> BasicAuth.decode(BasicAuth.encode(expectedCredential)))
                    .orElseAssert();
          assertThat(actualCredential.isPresent(), equalTo(true));
          assertThat(actualCredential.get(), equalTo(expectedCredential));
        });
  }
}
