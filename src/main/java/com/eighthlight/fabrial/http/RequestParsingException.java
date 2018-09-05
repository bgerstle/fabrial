package com.eighthlight.fabrial.http;

public class RequestParsingException extends Exception {
  public RequestParsingException(String message) {
    super(message);
  }
  public RequestParsingException(String message, Throwable t) {
    super(message, t);
  }
}
