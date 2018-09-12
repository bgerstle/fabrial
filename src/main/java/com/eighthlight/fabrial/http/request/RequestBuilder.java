package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.http.Method;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Optional;

public class RequestBuilder {
  private String version;
  private Method method;
  private URI uri;

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

  public Request build() {
    try {
      return new Request(Optional.ofNullable(version).orElseThrow(),
                         Optional.ofNullable(method).orElseThrow(),
                         Optional.ofNullable(uri).orElseThrow());
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
