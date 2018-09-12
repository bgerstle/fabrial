package com.eighthlight.fabrial.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

public class Response {
  public final String version;
  public final int statusCode;
  public final String reason;
  public final Map<String, String> headers;

  public Response(String version, int statusCode, String reason) {
    this(version, statusCode, reason, null);
  }

  public Response(String version, int statusCode, String reason, Map<String, String> headers) {
    this.version = Objects.requireNonNull(version);
    this.statusCode = statusCode;
    this.reason = reason;
    this.headers = Optional.ofNullable(headers).map(Map::copyOf).orElse(Map.of());

    if (!HttpVersion.allVersions.contains(this.version)) {
      throw new IllegalArgumentException(
          version + " is not one of the known HTTP version: " + HttpVersion.allVersions);
    }
    if (!(statusCode > 99 && statusCode < 1000)) {
      throw new IllegalArgumentException(
          statusCode + " is not a valid status code [100, 999]."
      );
    }
    if (reason != null
        && !reason.chars()
                 .allMatch(c -> StandardCharsets.US_ASCII.newEncoder().canEncode((char)c))) {
      // TODO: allow HTAB and opaque octets
      throw new IllegalArgumentException(
          reason + " contains illegal characters (must be ASCII)."
      );
    }
  }

  /**
   * Write the status line according to sect 3.1.2 which specifies ABNF:
   *
   *    status-line = HTTP-version SP status-code SP reason-phrase CRLF
   *
   * @throws IOException
   */
  private String getStatusLine() throws IOException {
    var builder = new StringBuilder();
    builder.append("HTTP/");
    builder.append(version);
    builder.append(" ");
    builder.append(statusCode);
    builder.append(" ");
    Optional.ofNullable(reason).ifPresent(builder::append);
    builder.append(CRLF);
    return builder.toString();
  }

  public void writeTo(OutputStream os) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    /*
      write the response according to RFC7230 section 3
      HTTP-message   = start-line
                      *( header-field CRLF )
                      CRLF
                      [ message-body ]
     */
    writer.write(getStatusLine());
    // ???: not sure why, but writer needs to be flushed in between components
    writer.flush();
    if (!headers.isEmpty()) {
      new HttpHeaderWriter(os).writeFields(headers);
      writer.write(CRLF);
    }
    writer.flush();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Response response = (Response) o;
    return statusCode == response.statusCode &&
           Objects.equals(version, response.version) &&
           Objects.equals(reason, response.reason) &&
           Objects.equals(headers, response.headers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, statusCode, reason, headers);
  }

  @Override
  public String toString() {
    return "Response{" +
           "version='" + version + '\'' +
           ", statusCode=" + statusCode +
           ", reason='" + reason + '\'' +
           ", headers=" + headers +
           '}';
  }
}
