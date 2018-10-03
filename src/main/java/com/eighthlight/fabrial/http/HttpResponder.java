package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;

public interface HttpResponder {
  public boolean matches(Request request);

  public Response getResponse(Request request);
}
