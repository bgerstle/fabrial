package com.eighthlight.fabrial.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

public final class RequestBuilder {
  private String version;
  private Method method;
  private URI uri;

  // Set version from a string with the format "HTTP/X.Y"
  public RequestBuilder withPrefixedVersion(String prefixedVersion) {
    String[] versionComponents = prefixedVersion.split("/");
    if (versionComponents.length < 2) {
      throw new IllegalArgumentException("Expected 'HTTP/X.Y', got: " + prefixedVersion);
    }
    return withVersion(versionComponents[1]);
  }

  public RequestBuilder withVersion(String version) {
    if (!HttpVersion.allVersions.contains(version)) {
      throw new IllegalArgumentException("Unexpected HTTP version: " + version);
    }
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
    this.uri = uri;
    return this;
  }

  public Request build() {
    return new Request(version, method, uri);
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
    return withRequestLine(firstLine.get()).build();
  }
}
