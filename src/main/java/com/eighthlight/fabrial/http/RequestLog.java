package com.eighthlight.fabrial.http;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class RequestLog {
  public final String version;
  public final Method method;
  public final URI uri;
  public final Map<String, String> headers;

  public RequestLog(String version, Method method, URI uri, Map<String, String> headers) {
    this.version = version;
    this.method = method;
    this.uri = uri;
    this.headers = headers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RequestLog that = (RequestLog) o;
    return Objects.equals(version, that.version) &&
           method == that.method &&
           Objects.equals(uri, that.uri) &&
           Objects.equals(headers, that.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, method, uri, headers);
  }

  @Override
  public String toString() {
    return "RequestLog{" +
           "version='" + version + '\'' +
           ", method=" + method +
           ", uri=" + uri +
           ", headers=" + headers +
           '}';
  }
}
