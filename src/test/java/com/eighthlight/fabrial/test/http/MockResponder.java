package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;

import java.net.URI;
import java.util.Objects;

public class MockResponder implements HttpResponder {
  public final URI targetURI;
  public final Method targetMethod;
  public final Response response;

  public MockResponder(URI targetURI, Method targetMethod, Response response) {
    this.targetURI = targetURI;
    this.targetMethod = targetMethod;
    this.response = response;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MockResponder that = (MockResponder) o;
    // intentionally excluding response
    return Objects.equals(targetURI, that.targetURI) &&
           Objects.equals(targetMethod, that.targetMethod);
  }

  @Override
  public String toString() {
    return "MockResponder{" +
           "targetURI=" + targetURI +
           ", targetMethod=" + targetMethod +
           ", response=" + response +
           '}';
  }

  @Override
  public int hashCode() {
    // intentionally excluding response
    return Objects.hash(targetURI, targetMethod);
  }

  @Override
  public boolean matches(Request request) {
    return request.uri.equals(targetURI) && request.method.equals(targetMethod);
  }

  @Override
  public Response getResponse(Request request) {
    return response;
  }
}
