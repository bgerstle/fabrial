package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.http.Method;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.NoSuchElementException;

public class RequestBuilder {
  private String version;
  private Method method;
  private URI uri;
  private InputStream body;
  private Map<String, String> headers;

  public RequestBuilder() {}

  // Set version from a string with the format "HTTP/X.Y"
  public RequestBuilder withPrefixedVersion(String prefixedVersion) {
    String[] versionComponents = prefixedVersion.split("/");
    if (versionComponents.length < 2) {
      throw new IllegalArgumentException("Expected 'HTTP/X.Y', got: " + prefixedVersion);
    }
    return withVersion(versionComponents[1]);
  }

  public RequestBuilder withVersion(String version) {
    this.version = version;
    return this;
  }

  public RequestBuilder withMethod(Method method) {
    this.method = method;
    return this;
  }

  public RequestBuilder withMethodValue(String methodStr) {
    return withMethod(Method.valueOf(methodStr));
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

  public RequestBuilder withHeaders(Map<String, String> headers) {
    this.headers = Map.copyOf(headers);
    return this;
  }

  public RequestBuilder withBody(InputStream body) {
    this.body = body;
    return this;
  }

  public Request build() {
    try {
      return new Request(version, method, uri, headers, body);
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
