package com.eighthlight.fabrial.http.response;

import com.eighthlight.fabrial.http.HttpVersion;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Response {
  public final String version;
  public final int statusCode;
  public final String reason;
  public final Map<String, String> headers;
  public final InputStream body;

  /**
   * Initialize a response object.
   * @param version     Version of the HTTP protocol (@see HttpVersion).
   * @param statusCode  Status code [100-999].
   * @param reason      Reason explaining the response (can be @code null).
   * @param headers     Map of response headers (can be @code null).
   * @param body        Stream of bytes to be sent as the body of the message (can be @code null).
   */
  public Response(String version,
                  int statusCode,
                  String reason,
                  Map<String, String> headers,
                  InputStream body) {
    this.version = Objects.requireNonNull(version);
    this.statusCode = statusCode;
    this.reason = reason;
    this.headers = Optional.ofNullable(headers).map(Map::copyOf).orElse(Map.of());
    this.body = body;

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
           Objects.equals(headers, response.headers) &&
           Objects.equals(body, response.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, statusCode, reason, headers, body);
  }

  @Override
  public String toString() {
    return "Response{" +
           "version='" + version + '\'' +
           ", statusCode=" + statusCode +
           ", reason='" + reason + '\'' +
           ", headers=" + headers +
           ", body=" + body +
           '}';
  }
}
