package com.eighthlight.fabrial.http.message;

public class MessageReaderException extends Exception {
  public MessageReaderException(Throwable t) {
    super(t);
  }

  public MessageReaderException(String message) {
    super(message);
  }

  public MessageReaderException(String message, Throwable t) {
    super(message, t);
  }
}
