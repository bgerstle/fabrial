package com.eighthlight.fabrial.http;

import java.io.*;
import java.util.Objects;
import java.util.Optional;

public class Response {
  public final String version;
  public final int statusCode;
  public final Optional<String> reason;

  public Response(String version, int statusCode, String reason) {
    this.version = version;
    this.statusCode = statusCode;
    this.reason = Optional.ofNullable(reason);
  }

  public void writeTo(OutputStream os) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    writer.write("HTTP/" + version);
    writer.write(" ");
    writer.write(Integer.toString(statusCode));
    writer.write(" ");
    if (reason.isPresent()) {
      writer.write(reason.get());
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
