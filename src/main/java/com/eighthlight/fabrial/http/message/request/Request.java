package com.eighthlight.fabrial.http.message.request;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Request {
  public final String version;
  public final String method;
  public final URI uri;
  public final Map<String, String> headers;
  public final InputStream body;

  public Request(String version,
                 String method,
                 URI uri) {
    this(version, method, uri, null, null);
  }

  public Request(String version,
                 String method,
                 URI uri,
                 Map<String, String> headers,
                 InputStream body) {
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
           Objects.equals(method, request.method) &&
           Objects.equals(uri, request.uri) &&
           Objects.equals(headers, request.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, method, uri, headers);
  }
}
