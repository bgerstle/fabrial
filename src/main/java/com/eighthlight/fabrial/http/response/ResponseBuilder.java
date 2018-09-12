package com.eighthlight.fabrial.http.response;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Builder object for @link Response instances.
 */
public class ResponseBuilder {
  private String version;
  private int statusCode;
  private String reason;
  private HashMap<String, String> headers;

  public ResponseBuilder() {
    headers = new HashMap<String, String>();
  }

  public ResponseBuilder withVersion(String version) {
    this.version = version;
    return this;
  }

  public ResponseBuilder withStatusCode(int statusCode) {
    this.statusCode = statusCode;
    return this;
  }

  public ResponseBuilder withReason(String reason) {
    this.reason = reason;
    return this;
  }

  public ResponseBuilder withHeaders(Map<String, String> headers) {
    this.headers = Optional.ofNullable(headers).map(HashMap::new).orElse(null);
    return this;
  }

  public ResponseBuilder withHeader(String name, String value) {
    headers.put(name, value);
    return this;
  }

  public Response build() {
    return new Response(version, statusCode, reason, headers, null);
  }
}
