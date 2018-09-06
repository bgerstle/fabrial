package com.eighthlight.fabrial.http;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Objects;

public class Request {
  public final String version;
  public final Method method;
  public final URI uri;

  public Request(String version, Method method, URI uri) {
    this.version = version;
    this.method = method;
    this.uri = uri;
  }
  
  @Override
  public String toString() {
    return "Request{" +
           "version='" + version + '\'' +
           ", method=" + method +
           ", uri=" + uri +
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
           Objects.equals(uri, request.uri);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, method, uri);
  }

  public void writeTo(OutputStream os) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    String line = String.join(" ", List.of(method.name(), uri.toString(), "HTTP/" + version))
                  + "\r\n";
    writer.write(line);
    writer.flush();
  }

  public static Request readFrom(InputStream stream) throws RequestParsingException {
    return new RequestBuilder().buildWithStream(stream);
  }
}
