package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Request;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

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
                  + "\r\n";
    writer.write(line);
    writer.flush();
  }
}
