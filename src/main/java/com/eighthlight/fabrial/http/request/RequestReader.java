package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.utils.HttpLineReader;
import com.eighthlight.fabrial.utils.Result;
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

  private static RequestBuilder withRequestLine(String requestLine) throws RequestParsingException {
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

  public Request readRequest() throws RequestParsingException {
    try {
      // must pass the reader through each step, otherwise the first reader
      // will consume the entire stream
      var lineReader = new HttpLineReader(is);
      return Result
          .attempt(() -> {
            var firstLine = lineReader.readLine();
            return Optional.ofNullable(firstLine)
                           .orElseThrow(() -> new RequestParsingException("Request is empty"));
          })
          .flatMapAttempt(RequestReader::withRequestLine)
          .flatMapAttempt(b -> {
            var headerReader = new HttpHeaderReader(is);
            var headers = headerReader.readHeaders();
            return b.withHeaders(headers);
          })
          .map(b -> {
            return b.withBody(is).build();
          })
          .orElseThrow();
    } catch (Exception e) {
      throw new RequestParsingException(e);
    }
  }
}
