package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.auth.BasicAuth;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.server.BasicAuthResponder;
import com.eighthlight.fabrial.server.Credential;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsMapContaining.hasEntry;

public class BasicAuthResponderTest {
  @Test
  void respondsWith401WithoutCallingResponderWhenHeaderMissing() {
    var credential = new Credential("user", "pass");
    var response = new BasicAuthResponder()
        .withExpectedCredential(credential)
        .withAuthorizedResponder(r -> {
          return new ResponseBuilder()
              .withVersion(r.version)
              .withStatusCode(200)
              .build();
        })
        .check(new RequestBuilder()
                   .withVersion(HttpVersion.ONE_ONE)
                   .withUriString("/")
                   .withMethod(Method.GET)
                   .build());
    assertThat(response.statusCode, equalTo(401));
    assertThat(response.headers, hasEntry("WWW-Authenticate", "Basic realm=\"default\""));
    assertThat(response.body, is(nullValue()));
  }

  @Test
  void respondsWith401WhenGivenInvalidCredentials() {
    var credential = new Credential("user", "pass");
    var response = new BasicAuthResponder()
        .withExpectedCredential(credential)
        .withAuthorizedResponder(r -> {
          return new ResponseBuilder()
              .withVersion(r.version)
              .withStatusCode(200)
              .build();
        })
        .check(new RequestBuilder()
                   .withVersion(HttpVersion.ONE_ONE)
                   .withUriString("/")
                   .withMethod(Method.GET)
                   .withHeaders(BasicAuth.encode(new Credential("admin", "1234")))
                   .build());
    assertThat(response.statusCode, equalTo(401));
    assertThat(response.body, is(nullValue()));
  }

  @Test
  void respondsWith400WhenCredentialParsingFails() {
    var credential = new Credential("user", "pass");
    var response = new BasicAuthResponder()
        .withExpectedCredential(credential)
        .withAuthorizedResponder(r -> {
          return new ResponseBuilder()
              .withVersion(r.version)
              .withStatusCode(200)
              .build();
        })
        .check(new RequestBuilder()
                   .withVersion(HttpVersion.ONE_ONE)
                   .withUriString("/")
                   .withMethod(Method.GET)
                   .withHeaders(Map.of("Authorization", "lol"))
                   .build());
    assertThat(response.statusCode, equalTo(400));
    assertThat(response.body, is(nullValue()));
  }

  @Test
  void delegatesToResponderWhenCredentialsAreValid() {
    var credential = new Credential("user", "pass");
    var request = new RequestBuilder()
        .withVersion(HttpVersion.ONE_ONE)
        .withUriString("/")
        .withMethod(Method.GET)
        .withHeaders(BasicAuth.encode(credential))
        .build();
    var expectedResponse = new ResponseBuilder()
        .withVersion(request.version)
        .withStatusCode(200)
        .build();
    var response = new BasicAuthResponder()
        .withExpectedCredential(credential)
        .withAuthorizedResponder(r -> expectedResponse)
        .check(request);
    assertThat(response, equalTo(expectedResponse));
  }
}
