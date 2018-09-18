package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;

import java.io.Reader;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Request {
  public final String version;
  public final Method method;
  public final URI uri;
  public final Map<String, String> headers;
  public final Reader body;

  public Request(String version,
                 Method method,
                 URI uri) {
    this(version, method, uri, null, null);
  }

  public Request(String version,
                 Method method,
                 URI uri,
                 Map<String, String> headers,
                 Reader body) {
    if (!HttpVersion.allVersions.contains(version)) {
      throw new IllegalArgumentException("Unexpected HTTP version: " + version);
    }
    this.version = Objects.requireNonNull(version);
    this.method = Objects.requireNonNull(method);
    this.uri = Objects.requireNonNull(uri);
    this.headers = Optional.ofNullable(headers).map(Map::copyOf).orElse(Map.of());
    this.body = body;
  }

  @Override
  public String toString() {
    return "Request{" +
           "version='" + version + '\'' +
           ", method=" + method +
           ", uri=" + uri +
           ", headers=" + headers +
           '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Request request = (Request) o;
    return Objects.equals(version, request.version) &&
           method == request.method &&
           Objects.equals(uri, request.uri) &&
           Objects.equals(headers, request.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, method, uri, headers);
  }
}
