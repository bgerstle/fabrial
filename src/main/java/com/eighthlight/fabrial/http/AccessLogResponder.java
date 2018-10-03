package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.stream.Collectors;

public class AccessLogResponder implements HttpResponder {
  private final AccessLogger accessLogger;

  public AccessLogResponder(AccessLogger accessLogger) {
    this.accessLogger = accessLogger;
  }

  @Override
  public boolean matches(Request request) {
    return request.uri.getPath().equals("/logs");
  }

  @Override
  public Response getResponse(Request request) {
    if (!request.method.equals(Method.GET)) {
      return new ResponseBuilder()
          .withVersion(request.version)
          .withStatusCode(405)
          .build();
    }
    var body =
        accessLogger
            .loggedRequests()
            .stream()
            .map(r -> String.join(" ",
                                  r.method.name(),
                                  r.uri.getPath().toString(),
                                  r.version))
            .collect(Collectors.joining("\n"))
            .getBytes();
    return new ResponseBuilder()
        .withVersion(request.version)
        .withStatusCode(200)
        .withHeaders(Map.of(
            "Content-Length", Integer.toString(body.length),
            "Content-Type", "text/plain"
        ))
        .withBody(new ByteArrayInputStream(body))
        .build();
  }
}
