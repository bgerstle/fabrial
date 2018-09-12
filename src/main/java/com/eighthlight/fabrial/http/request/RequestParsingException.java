package com.eighthlight.fabrial.http.request;

public class RequestParsingException extends Exception {
  public RequestParsingException(Throwable t) {
    super(t);
  }

  public RequestParsingException(String message) {
    super(message);
  }

  public RequestParsingException(String message, Throwable t) {
    super(message, t);
  }
}
