package com.eighthlight.fabrial.test.http.client;

import com.eighthlight.fabrial.http.message.MessageReaderException;
import com.eighthlight.fabrial.http.message.request.MessageReader;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ResponseReader extends MessageReader<Response, ResponseBuilder> {
  private static ResponseBuilder responseBuilderForLine(String statusLine) throws MessageReaderException {
    if (statusLine.startsWith(" ")) {
      throw new MessageReaderException("Requests should not have leading whitespace");
    }
    Scanner statusLineScanner = new Scanner(statusLine).useDelimiter(" ");
    try {
      return new ResponseBuilder()
          .withPrefixedVersion(statusLineScanner.next())
          .withStatusCode(Integer.parseInt(statusLineScanner.next()))
          .withReason(statusLineScanner.hasNext() ? statusLineScanner.next() : null);
    } catch (NumberFormatException e) {
      throw new MessageReaderException("Invalid status code", e);
    } catch (NoSuchElementException e) {
      throw new MessageReaderException(
          "Malformed status line. Expected space-separated HTTP version, status code, and reason, but got "
          + statusLine);
    } catch (IllegalArgumentException e) {
      throw new MessageReaderException("Invalid input detected", e);
    }
  }

  public ResponseReader(InputStream is) {
    super(ResponseReader::responseBuilderForLine, is);
  }
}
