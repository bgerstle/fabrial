package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.Request;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

public class RequestWriter {
  private final OutputStream os;

  public RequestWriter(OutputStream os) {
    this.os = os;
  }

  public void writeRequest(Request request) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    List<String> requestLineComponents = List.of(request.method.name(),
                                                 request.uri.toString(),
                                                 "HTTP/" + request.version);
    String line = String.join(" ", requestLineComponents)
                  + CRLF;
    writer.write(line);
    writer.flush();
  }
}
