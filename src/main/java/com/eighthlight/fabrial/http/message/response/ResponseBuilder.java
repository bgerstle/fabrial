package com.eighthlight.fabrial.http.message.response;

import com.eighthlight.fabrial.http.message.AbstractMessageBuilder;

import java.io.ByteArrayInputStream;

/**
 * Builder object for @link Response instances.
 */
public class ResponseBuilder extends AbstractMessageBuilder<ResponseBuilder, Response> {
  private int statusCode;
  private String reason;

  public ResponseBuilder() {
    super();
  }

  public ResponseBuilder(Response response) {
    super(response.version, response.headers, response.body);
    this.statusCode = response.statusCode;
    this.reason = response.reason;
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
