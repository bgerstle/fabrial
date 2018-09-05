package com.eighthlight.fabrial.http;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

public class Request {
  public final String version;
  public final Method method;
  public final URI uri;

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

  @Override
  public String toString() {
    return "Request{" +
           "method=" + method +
           ", uri=" + uri +
           '}';
  }

  public void writeTo(OutputStream os) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    String line = String.join(" ", List.of(method.name(), uri.toString(), "HTTP/" + version))
                  + "\r\n";
    writer.write(line);
    writer.flush();
  }

  public static Request readFrom(InputStream stream) throws IOException, RequestParsingException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    // TODO: skip up to N empty lines before throwing bad request
    // TODO: check for leading whitespace in request line
    String requestLine = reader.readLine();
    String[] components = requestLine.split(" ");
    if (components.length < 3) {
      throw new RequestParsingException(
          "Request line missing components (expected method, uri, and HTTP version.");
    }
    Method method;
    try {
      method = Method.valueOf(components[0]);
    } catch (IllegalArgumentException e) {
      throw new RequestParsingException("Invalid HTTP method", e);
    }
    URI uri;
    try {
      uri = new URI(components[1]);
    } catch (URISyntaxException e) {
      throw new RequestParsingException("Failed to parse URI", e);
    }
    String version;
    String versionComponent = components[2];
    String[] versionSubcomponents = versionComponent.split("/");
    if (versionSubcomponents.length < 2) {
      throw new RequestParsingException("Malformed HTTP version: " + versionComponent);
    }
    version = versionSubcomponents[1];
    return new Request(version, method, uri);
  }

  public Request(String version, Method method, URI uri) {
    this.version = version;
    this.method = method;
    this.uri = uri;
  }
}
