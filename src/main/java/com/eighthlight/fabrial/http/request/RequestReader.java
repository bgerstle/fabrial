package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.utils.HttpLineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

public class RequestReader {
  private static final Logger logger = LoggerFactory.getLogger(RequestReader.class);

  private final InputStream is;

  public RequestReader(InputStream is) {
    this.is = is;
  }

  private static RequestBuilder builderWithRequestLine(String requestLine) throws RequestParsingException {
    if (requestLine.isEmpty()) {
      throw new RequestParsingException("Request is empty.");
    } else if (requestLine.startsWith(" ")) {
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

  public Optional<Request> readRequest() throws RequestParsingException {
    try {
      // must pass the reader through each step, otherwise the first reader
      // will consume the entire stream
      var lineReader = new HttpLineReader(is);
      var firstLine = lineReader.readLine();
      if (firstLine == null || firstLine.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(
          RequestReader.builderWithRequestLine(firstLine)
                       .withHeaders(new HttpHeaderReader(is).readHeaders())
                       .withBody(is)
                       .build()
      );
    } catch (Exception e) {
      throw new RequestParsingException(e);
    }
  }
}
