package com.eighthlight.fabrial.http;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Serialize map of header fields associated with an HTTP message.
 *
 * As specified in RFC7320 Section 3.2:
 *
 *      header-field   = field-name ":" OWS field-value OWS
 *      field-name     = token
 *      field-value    = *(field-content / obs-fold)
 *      field-content  = field-vchar [ 1*( SP / HTAB ) field-vchar ]
 *      field-vchar    = VCHAR / obs-text
 *      obs-fold       = CRLF 1*( SP / HTAB )
 *                     ; obsolete line folding
 *
 * Where:
 *      token = 1*tchar
 *      tchar = "!" / "#" / "$" / "%" / "&" / "’" / "*" / "+" / "-" / "." /
 *  "^" / "_" / "‘" / "|" / "~" / DIGIT / ALPHA
 *      OWS = *( SP / HTAB )
 *
 *  and "VCHAR" is any ASCII character.
 *
 *  This implementation will defer on obsolete folding (since it's obsolete)...
 */
public class HttpHeaderWriter {
  private final BufferedWriter os;

  public HttpHeaderWriter(OutputStream os) {
    this.os = new BufferedWriter(new OutputStreamWriter(os));
  }


  public void writeField(String name, String value) {

  }

  public void writeFields(Map<String, String> fields) {

  }
}
