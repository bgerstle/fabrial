package com.eighthlight.fabrial.http;

public interface HttpResponder {
  public boolean matches(Request request);

  public Response getResponse(Request request);
}
