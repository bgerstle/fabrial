package com.eighthlight.fabrial.http.request;

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

  private void skipNewline(Reader reader) {
    try {
      char[] buf = new char[1];
      int read = reader.read(buf, 0, 1);
      if (read == -1) {
        logger.trace("Encountered EOF after header fields instead of CRLF.");
      } else {
        var readChars = new String(buf);
        if (!readChars.equals("\n")) {
          logger.warn("Found non-newline character after header fields: " + readChars);
        }
      }
    } catch (IOException e) {
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
          .flatMapAttempt(b -> {
            var headerReader = new HttpHeaderReader(reader);
            var headers = headerReader.readHeaders();
            return b.withHeaders(headers);
          })
          .map(b -> {
            var contentLength = Optional.ofNullable(b.headers.get("Content-Length"));
            contentLength.flatMap((lenStr) -> {
              return Result.attempt(() -> Long.decode(lenStr)).toOptional();
            }).ifPresent((len) -> {
              b.withBody(reader);
            });
            return b.build();
          })
          .orElseThrow();
    } catch (Exception e) {
      throw new RequestParsingException(e);
    }
  }
}
