package com.eighthlight.fabrial.http;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Stream;

public class Request {
  public final String version;
  public final Method method;
  public final URI uri;

  public static RequestBuilder builder() {
    return new RequestBuilder();
  }

  public static class RequestBuilder {
    private String version;
    private Method method;
    private URI uri;

    private RequestBuilder() {}

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

    public RequestBuilder withRequestLine(String requestLine) throws RequestParsingException {
      if (requestLine.startsWith(" ")) {
        throw new RequestParsingException("Requests should not have leading whitespace");
      }
      Scanner requestLineScanner = new Scanner(requestLine).useDelimiter(" ");
      try {
        return withMethodValue(requestLineScanner.next())
            .withUriString(requestLineScanner.next())
            .withPrefixedVersion(requestLineScanner.next());
      } catch (NoSuchElementException e) {
        throw new RequestParsingException(
            "Malformed request line. Expected space-separated method, uri, and HTTP version, but got "
            + requestLine);
      } catch (IllegalArgumentException e) {
        throw new RequestParsingException("Invalid input detected", e);
      }
    }

    public Request buildWithStream(InputStream stream) throws RequestParsingException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      Stream<String> lines = reader.lines();
      Optional<String> firstLine = lines.findFirst();
      if (!firstLine.isPresent()) {
        throw new RequestParsingException("Request is empty");
      }
      try {
        return withRequestLine(firstLine.get()).build();
      } catch (Exception e) {
        throw new RequestParsingException(e);
      }
    }
  }


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
}
