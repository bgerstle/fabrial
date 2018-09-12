package com.eighthlight.fabrial.http.request;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Stream;

public class RequestReader {
  private final InputStream is;

  public RequestReader(InputStream is) {
    this.is = is;
  }

  private static RequestBuilder withRequestLine(String requestLine) throws RequestParsingException {
    if (requestLine.startsWith(" ")) {
      throw new RequestParsingException("Requests should not have leading whitespace");
    }
    Scanner requestLineScanner = new Scanner(requestLine).useDelimiter(" ");
    try {
      return new RequestBuilder()
                    .withMethodValue(requestLineScanner.next())
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

  public Request readRequest() throws RequestParsingException {
    Optional<String> firstLine;
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      Stream<String> lines = reader.lines();
      firstLine = lines.findFirst();
      if (!firstLine.isPresent()) {
        throw new RequestParsingException("Request is empty");
      }
    } catch (Exception e) {
      throw new RequestParsingException(e);
    }
    try {
      return withRequestLine(firstLine.get()).build();
    } catch (Exception e) {
      throw new RequestParsingException(e);
    }
  }
}
