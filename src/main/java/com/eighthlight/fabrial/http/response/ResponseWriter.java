package com.eighthlight.fabrial.http.response;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

public class ResponseWriter {
  private final OutputStream os;

  public ResponseWriter(OutputStream os) {
    this.os = os;
  }

  public void writeResponse(Response response) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
    /*
      write the response according to RFC7230 section 3
      HTTP-message   = start-line
                      *( header-field CRLF )
                      CRLF
                      [ message-body ]
     */
    new HttpStatusLineWriter(os).writeStatusLine(response.version,
                                                 response.statusCode,
                                                 response.reason);
    if (!response.headers.isEmpty()) {
      new HttpHeaderWriter(os).writeFields(response.headers);
      writer.write(CRLF);
    }
    writer.flush();
    if (response.body != null) {
      response.body.transferTo(os);
      os.flush();
    }
  }
}
