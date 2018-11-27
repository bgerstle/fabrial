package com.eighthlight.fabrial.http.message.request;

import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.AbstractMessageBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;

public class RequestBuilder extends AbstractMessageBuilder<RequestBuilder, Request> {
  public String method;
  public URI uri;

  public RequestBuilder() {}

  public RequestBuilder withMethod(Method method) {
    return withMethodValue(method.name());
  }

  public RequestBuilder withMethodValue(String methodStr) {
    this.method = methodStr;
    return this;
  }

  public RequestBuilder withUriString(String uriString) {
    try {
      return withUri(new URI(uriString));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public RequestBuilder withUri(URI uri) {
    if (uri.toString().isEmpty()) {
      throw new IllegalArgumentException("URI request targets must not be empty.");
    }
    this.uri = uri;
    return this;
  }

  @Override
  public Request build() {
    try {
      return new Request(version, method, uri, headers, body);
    } catch (NoSuchElementException|NullPointerException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
