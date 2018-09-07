package com.eighthlight.fabrial.http;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public class Response {
  public final String version;
  public final int statusCode;
  public final Optional<String> reason;

  public Response(String version, int statusCode, String reason) {
    this.version = version;
    this.statusCode = statusCode;
    this.reason = Optional.ofNullable(reason).filter(s -> !s.isEmpty());

    if (!HttpVersion.allVersions.contains(this.version)) {
      throw new IllegalArgumentException(
          version + " is not one of the known HTTP version: " + HttpVersion.allVersions);
    }
    if (!(statusCode > 99 && statusCode < 1000)) {
      throw new IllegalArgumentException(
          statusCode + " is not a valid status code [100, 999]."
      );
    }
    if (this.reason.isPresent()
        && !this.reason.get().chars().allMatch(c -> StandardCharsets.US_ASCII.newEncoder().canEncode((char)c))) {
      // TODO: allow HTAB and opaque octets
      throw new IllegalArgumentException(
          reason + " contains illegal characters (must be ASCII)."
      );
    }
  }

  public void writeTo(OutputStream os) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));

    // write the status line according to sect 3.1.2 which specifies ABNF:
    //
    //    status-line = HTTP-version SP status-code SP reason-phrase CRLF
    //
    writer.write("HTTP/" + version);
    writer.write(" ");
    writer.write(Integer.toString(statusCode));
    writer.write(" ");
    if (reason.isPresent()) {
      writer.write(reason.get());
      // no space after reason (though reason could contain a space)
    }
    writer.write("\r\n");
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
           Objects.equals(reason, response.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, statusCode, reason);
  }

  @Override
  public String toString() {
    return "Response{" +
           "version='" + version + '\'' +
           ", statusCode=" + statusCode +
           ", reason=" + reason +
           '}';
  }
}
