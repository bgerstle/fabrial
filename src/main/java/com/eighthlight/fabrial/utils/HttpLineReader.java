package com.eighthlight.fabrial.utils;

import java.io.IOException;
import java.io.InputStream;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

/**
 * Given an input stream, read ASCII strings separated by CRLF.
 *
 * Note that this assumes the data retrieved from the input stream is ASCII, since the HTTP message
 * components are limited to ASCII.
 */
public class HttpLineReader  {
  private final InputStream inputStream;

  public HttpLineReader(InputStream readable) {
    this.inputStream = readable;
  }

  public String readLine() throws IOException {
    var builder = new StringBuilder();
    var readChar = inputStream.read();
    while (readChar != -1) {
      builder.appendCodePoint(readChar);
      var crlfIndex = builder.lastIndexOf(CRLF);
      if (crlfIndex != -1) {
        // remove trailing newline, just like Scanner/BufferedReader would
        builder.replace(crlfIndex, crlfIndex + CRLF.length(), "");
        break;
      }
      readChar = inputStream.read();
    }
    return builder.toString();
  }
}
