package com.eighthlight.fabrial.http.auth;

public class AuthorizationParsingException extends Exception {
  private AuthorizationParsingException() {
    super();
  }

  public static AuthorizationParsingException failedToMatch() {
    return new AuthorizationParsingException("Failed to parse credentials from Authorization header");
  }

  public static AuthorizationParsingException emptyCredentials() {
    return new AuthorizationParsingException("Credentials were empty.");
  }

  public static AuthorizationParsingException malformedCredentials(Throwable t) {
    return new AuthorizationParsingException("Credentials should be in the format user:password", t);
  }

  private AuthorizationParsingException(String message) {
    super(message);
  }

  private AuthorizationParsingException(String message, Throwable cause) {
    super(message, cause);
  }

  private AuthorizationParsingException(Throwable cause) {
    super(cause);
  }
}
