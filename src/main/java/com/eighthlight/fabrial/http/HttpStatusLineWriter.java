package com.eighthlight.fabrial.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

/**
 * Class responsible for writing the "status-line" component of an HTTP response.
 */
public class HttpStatusLineWriter {
  private final OutputStream os;

  public HttpStatusLineWriter(OutputStream os) {
    this.os = os;
  }

  /**
   * Write the status line according to sect 3.1.2 which specifies ABNF:
   *
   *    status-line = HTTP-version SP status-code SP reason-phrase CRLF
   *
   * @throws IOException
   */
  public void writeStatusLine(@NotNull String version,
                              int statusCode,
                              @Nullable String reason) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    writer.write("HTTP/");
    writer.write(Objects.requireNonNull(version));
    writer.write(" ");
    writer.write(Integer.toString(statusCode));
    writer.write(" ");
    if (reason != null) {
      writer.write(reason);
    }
    writer.write(CRLF);
    writer.flush();
  }
}
