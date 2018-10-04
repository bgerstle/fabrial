package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.auth.AuthorizationParsingException;
import com.eighthlight.fabrial.http.auth.BasicAuth;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Handles checking HTTP requests for "admin" credentials, with a callback for handling
 * authorized requests.
 */
public class BasicAuthResponder {
  private Credential expectedCredential;
  private Function<Request, Response> responder;

  public BasicAuthResponder withExpectedCredential(Credential expectedCredential) {
    this.expectedCredential = Objects.requireNonNull(expectedCredential);
    return this;
  }

  public BasicAuthResponder withAuthorizedResponder(Function<Request, Response> responder) {
    this.responder = responder;
    return this;
  }

  public Response check(Request request) {
    assert this.expectedCredential != null;
    assert this.responder != null;
    try {
      var requestCredential = BasicAuth.decode(request.headers).get();
      if (requestCredential.equals(this.expectedCredential)) {
        return responder.apply(request);
      } else {
        return new ResponseBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withStatusCode(401)
            .build();
      }
    } catch (NoSuchElementException e) {
      return new ResponseBuilder()
          .withVersion(HttpVersion.ONE_ONE)
          .withStatusCode(401)
          .withHeader("WWW-Authenticate", "Basic realm=\"default\"")
          .build();
    } catch (AuthorizationParsingException e) {
      return new ResponseBuilder()
          .withVersion(HttpVersion.ONE_ONE)
          .withStatusCode(400)
          .withReason("Failed to parse authentication headers: " + e.getMessage())
          .build();
    }
  }
}
