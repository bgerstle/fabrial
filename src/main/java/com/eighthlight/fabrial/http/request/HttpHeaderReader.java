package com.eighthlight.fabrial.http.request;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;

/**
 * Scans an input string for header fields and values.
 *
 * Based on RFC7320 Section 3.2:
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
 *  and "VCHAR" is any visible ASCII character. Field values also can't contain delimiters (e.g. ":").
 *
 */
public class HttpHeaderReader {
  private static final Pattern FIELD_NAME_PATTERN = Pattern.compile("([!#$%&'*+-.\\^_'|~0-9a-zA-Z]+):");
  private static final Pattern FIELD_VALUE_PATTERN = Pattern.compile(" *([^:]+)");

  private final Scanner scanner;

  public HttpHeaderReader(InputStream is) {
    this.scanner = new Scanner(is);
  }

  public String nextFieldName() {
    if (hasNext()) {
      scanner.findInLine(FIELD_NAME_PATTERN);
      return scanner.match().group(1);
    }
    return null;
  }

  public String nextFieldValue() {
    if (scanner.hasNext(FIELD_VALUE_PATTERN)) {
      scanner.findInLine(FIELD_VALUE_PATTERN);
      return scanner.match().group(1).trim();
    }
    return null;
  }

  public void skipToNextLine() {
    scanner.skip(" *"+CRLF);
  }

  public boolean hasNext() {
    return scanner.hasNext(FIELD_NAME_PATTERN);
  }

  public Map<String, String> readHeaders() {
    var headers = new HashMap<String, String>();
    while (hasNext()) {
      headers.put(nextFieldName(), nextFieldValue());
      skipToNextLine();
    }
    return headers;
  }
}
