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

  /**
   * Initialize a response object.
   * @param version     Version of the HTTP protocol (@see HttpVersion).
   * @param statusCode  Status code [100-999].
   * @param reason      Reason explaining the response (can be @code null).
   * @param headers     Map of response headers (can be @code null).
   */
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


  public void writeTo(OutputStream os) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    /*
      write the response according to RFC7230 section 3
      HTTP-message   = start-line
                      *( header-field CRLF )
                      CRLF
                      [ message-body ]
     */
    new HttpStatusLineWriter(os).writeStatusLine(version, statusCode, reason);
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
