package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.response.Response;

public interface HttpResponder {
  public boolean matches(Request request);

  public Response getResponse(Request request);
}
