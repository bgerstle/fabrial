package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.utils.Result;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

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
    try {
      // must pass the reader through each step, otherwise the first reader
      // will consume the entire stream
      var reader = new BufferedReader(new InputStreamReader(is));
      return Result
          .attempt(() -> {

            var firstLine = reader.readLine();
            return Optional.ofNullable(firstLine)
                           .orElseThrow(() -> new RequestParsingException("Request is empty"));
          })
          .flatMapAttempt(RequestReader::withRequestLine)
          .map(b -> {
            var headerReader = new HttpHeaderReader(reader);
            var headers = headerReader.readHeaders();
            return b.withHeaders(headers).build();
          })
          .orElseThrow();
    } catch (Exception e) {
      throw new RequestParsingException(e);
    }
  }
}
