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

  public Request(String version, Method method, URI uri) {
    if (!HttpVersion.allVersions.contains(version)) {
      throw new IllegalArgumentException("Unexpected HTTP version: " + version);
    }
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

  public static Request readFrom(InputStream stream) throws IOException, RequestParsingException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
    String requestLine = reader.readLine();

    String[] components = requestLine.split(" ");
    // Checking for `< 3` to permit extra whitespace before CRLF
    if (components.length < 3) {
      throw new RequestParsingException(
          "Malformed request line. Expected space-separated method, uri, and HTTP version, but got "
          + requestLine);
    }

    String method = components[0];

    URI uri;
    try {
      uri = new URI(components[1]);
    } catch (URISyntaxException e) {
      throw new RequestParsingException("Failed to parse URI", e);
    }

    String versionComponent = components[2];
    String[] versionSubcomponents = versionComponent.split("/");
    if (versionSubcomponents.length < 2) {
      throw new RequestParsingException("Malformed HTTP version: " + versionComponent);
    }
    String version = versionSubcomponents[1];

    try {
      return new Request(version, Method.valueOf(method), uri);
    } catch (IllegalArgumentException e) {
      throw new RequestParsingException("Failed to construct request", e);
    }
  }
}
