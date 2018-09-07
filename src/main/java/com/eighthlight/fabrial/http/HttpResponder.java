package com.eighthlight.fabrial.http;

public interface HttpResponder {
  public boolean matches(Request request);

  public Response responseFor(Request request);
}
