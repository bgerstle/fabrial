package com.eighthlight.fabrial.http.message.request;

import com.eighthlight.fabrial.http.message.MessageReader;
import com.eighthlight.fabrial.http.message.MessageReaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

public class RequestReader extends MessageReader<Request, RequestBuilder> {
  private static final Logger logger = LoggerFactory.getLogger(RequestReader.class);

  private static RequestBuilder requestBuilderForLine(String requestLine) throws MessageReaderException {
    if (requestLine.startsWith(" ")) {
      throw new MessageReaderException("Requests should not have leading whitespace");
    }
    Scanner requestLineScanner = new Scanner(requestLine).useDelimiter(" ");
    try {
      return new RequestBuilder()
          .withMethodValue(requestLineScanner.next())
          .withUriString(requestLineScanner.next())
          .withPrefixedVersion(requestLineScanner.next());
    } catch (NoSuchElementException e) {
      throw new MessageReaderException(
          "Malformed request line. Expected space-separated method, uri, and HTTP version, but got "
          + requestLine);
    } catch (IllegalArgumentException e) {
      throw new MessageReaderException("Invalid input detected", e);
    }
  }

  public RequestReader(InputStream is) {
    super(RequestReader::requestBuilderForLine, is);
  }

  public Optional<Request> readRequest() throws MessageReaderException {
    return read();
  }
}
