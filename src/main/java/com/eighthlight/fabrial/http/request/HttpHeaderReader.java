package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.utils.Result;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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
  private static final Pattern HEADER_LINE_PATTERN =
      Pattern.compile("([!#$%&'*+-.\\^_'|~0-9a-zA-Z]+): *(.*)");

  // Invoke a scanner method reference, wrapping any thrown exceptions and returning null on error.
  private static <ArgType, RetType> RetType scanSafely(Function<ArgType, RetType> scanf, ArgType arg) {
    return Result.attempt(() -> Optional.ofNullable(scanf.apply(arg)))
                 .toOptional()
                 // extract nested optional, defaulting to "empty" if an error occurred
                 .orElse(Optional.empty())
                 .orElse(null);
  }

  private final Reader reader;

  public HttpHeaderReader(Reader reader) {
    this.reader = reader;
  }

  private String nextLine() throws IOException {
    var builder = new StringBuilder();
    var readChar = reader.read();
    while (readChar != -1) {
      builder.appendCodePoint(readChar);
      var crlfIndex = builder.lastIndexOf(CRLF);
      if (crlfIndex != -1) {
        // remove trailing newline, just like Scanner/BufferedReader would
        builder.replace(crlfIndex, crlfIndex + CRLF.length(), "");
        break;
      }
      readChar = reader.read();
    }
    return builder.toString();
  }

  public Map<String, String> readHeaders() throws IOException {
    var headers = new HashMap<String, String>();
    var line = nextLine();
    while (!line.isEmpty()) {
      var matcher = HEADER_LINE_PATTERN.matcher(line);
      if (matcher.matches() && matcher.groupCount() > 1) {
        headers.put(matcher.group(1), matcher.groupCount() == 2 ? matcher.group(2).trim() : "");
      }
      line = nextLine();
    }
    return headers;
  }
}
