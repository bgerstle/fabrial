package com.eighthlight.fabrial.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

public class HttpLineReader  {
  private final InputStream inputStream;
  private final ByteBuffer charByteBuffer;

  public HttpLineReader(InputStream readable) {
    this.inputStream = readable;
    this.charByteBuffer = ByteBuffer.allocate(4);
  }

  private Integer read() throws IOException {
    charByteBuffer.clear();
    var readResult = inputStream.read(charByteBuffer.array(), 0, 4);
    if (readResult == -1) {
      return null;
    }
    return charByteBuffer.asIntBuffer().get();
  }

  public String readLine() throws IOException {
    var builder = new StringBuilder();
    var readChar = read();
    while (readChar != null) {
      builder.appendCodePoint(readChar);
      var crlfIndex = builder.lastIndexOf(CRLF);
      if (crlfIndex != -1) {
        // remove trailing newline, just like Scanner/BufferedReader would
        builder.replace(crlfIndex, crlfIndex + CRLF.length(), "");
        break;
      }
      readChar = read();
    }
    return builder.toString();
  }
}
