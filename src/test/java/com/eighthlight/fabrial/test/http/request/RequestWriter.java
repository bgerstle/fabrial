package com.eighthlight.fabrial.test.http.request;

import com.eighthlight.fabrial.http.message.request.Request;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

public class RequestWriter {
  private final OutputStream os;
  private final BufferedWriter writer;

  public RequestWriter(OutputStream os) {
    this.os = os;
    this.writer = new BufferedWriter(new OutputStreamWriter(os));
  }

  public void writeRequest(Request request) throws IOException {
    writeRequestLine(request);
    writer.write(CRLF);
    writeHeaders(request);
    writer.write(CRLF);
    writer.flush();
    if (request.body != null) {
      request.body.transferTo(os);
      os.flush();
    }
  }

  public void writeRequestLine(Request request) throws IOException {
    List<String> requestLineComponents = List.of(request.method.name(),
                                                 request.uri.toString(),
                                                 "HTTP/" + request.version);
    writer.write(String.join(" ", requestLineComponents));
  }

  public void writeHeaders(Request request) throws IOException {
    if (request.headers == null) {
      return;
    }
    for (var entry: request.headers.entrySet()) {
      writer.write(entry.getKey());
      writer.write(":");
      writer.write(entry.getValue());
      writer.write(CRLF);
    }
  }
}
