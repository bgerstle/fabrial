package com.eighthlight.fabrial.http.message.response;

import com.eighthlight.fabrial.http.message.AbstractMessageBuilder;

import java.io.ByteArrayInputStream;

/**
 * Builder object for @link Response instances.
 */
public class ResponseBuilder extends AbstractMessageBuilder<ResponseBuilder, Response> {
  private String version;
  private int statusCode;
  private String reason;

  public ResponseBuilder() {
    super();
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

  public ResponseBuilder withHeader(String name, String value) {
    headers.put(name, value);
    return this;
  }

  public ResponseBuilder withBodyFromString(String bodyStr) {
    this.body = new ByteArrayInputStream(bodyStr.getBytes());
    return this;
  }

  @Override
  public Response build() {
    return new Response(version, statusCode, reason, headers, body);
  }
}
