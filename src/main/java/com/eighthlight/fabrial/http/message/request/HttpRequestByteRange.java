package com.eighthlight.fabrial.http.message.request;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Value representing the first & last bytes of an HTTP range request.
 *
 * All ranges are inclusive.
 */
public class HttpRequestByteRange {
  public final int first;
  public final int last;

  public static final class ParsingException extends Exception {
    public ParsingException(String message) {
      super(message);
    }

    public ParsingException(String message, Throwable cause) {
      super(message, cause);
    }

    public ParsingException(Throwable cause) {
      super(cause);
    }
  }

  private static Optional<Integer> parseRangeComponent(String str) {
    return Optional.ofNullable(str)
                   .flatMap(s -> s.isEmpty() ? Optional.empty() : Optional.of(s))
                   .map(Integer::parseInt);
  }

  private static final Pattern RANGE_PATTERN = Pattern.compile("([^=]+)=(\\d*)-(\\d*)$");

  /**
   * Parse a byte range header field, as specified in HTTP/1.1 Range Requests RFC 7233:
   *
   *     byte-ranges-specifier = bytes-unit "=" byte-range-set
   *     byte-range-set  = 1#( byte-range-spec / suffix-byte-range-spec )
   *     byte-range-spec = first-byte-pos "-" [ last-byte-pos ]
   *     first-byte-pos  = 1*DIGIT
   *     last-byte-pos   = 1*DIGIT
   *
   * Which describes ranges such as "bytes=N-M" or "bytes=N-", that can be interpreted as
   * "bytes N through M" and "all bytes after N" respectively. There's also a "suffix" spec:
   *
   *     suffix-byte-range-spec = "-" suffix-length
   *     suffix-length = 1*DIGIT
   *
   * An example being: "bytes=-5", interpreted as "the last five bytes of the file".
   * @param headerValue The value of the "Range" header field of an HTTP request.
   * @param fileSize    The size of the file targeted by the request. Used for bounds checking.
   * @return A range object initialized with first & last values, including any computations
   *         necessary to calculate effective first & last for partially-specified ranges
   *         (e.g. "5-" or "-10").
   * @throws ParsingException If the range is malformed in any way or the first/last positions are
   *                          out of bounds.
   */
  public static HttpRequestByteRange parseFromHeader(String headerValue,
                                                     int fileSize) throws ParsingException {
    var matcher = RANGE_PATTERN.matcher(headerValue);
    if (!matcher.find()) {
      throw new ParsingException("Failed to match range components. Expected 'bytes=[first]-[last]'");
    }

    var unit = matcher.group(1);
    if (!"bytes".equals(unit)) {
      throw new ParsingException("Unacceptable range request unit: " + unit);
    }

    try {
      var first = parseRangeComponent(matcher.group(2));

      // wrap last to max index, see RFC 7233 section 2.1:
      // "if the last-byte value...is greater than or equal to the length...of the data, the byte range
      // is interpreted as the remainder of the representation"
      var last = parseRangeComponent(matcher.group(3)).map(l -> Integer.min(l, fileSize - 1));

      if (first.isPresent() && last.isPresent()) {
        // return range w/ desired first & last positions (e.g. "0-5")
        return new HttpRequestByteRange(first.get(), last.get());
      } else if (first.isPresent()) {
        // return range from desired first position to end of file (e.g. "5-")
        return new HttpRequestByteRange(first.get(), fileSize - 1);
      } else if (last.isPresent()) {
        if (last.get() < 1) {
          throw new ParsingException("Suffix range of '-0' is invalid.");
        }
        // return range for the desired trailing bytes of a file (e.g. "-5")
        return new HttpRequestByteRange(fileSize - last.get(), fileSize - 1);
      } else {
        throw new ParsingException("Ranges must have at least one component: X-Y, X-, or -Y.");
      }
    } catch (IllegalArgumentException e) {
      throw new ParsingException(e);
    }
  }

  public HttpRequestByteRange(int first, int last) {
    if (first < 0) {
      throw new IllegalArgumentException("First position must be greater than or equal to zero.");
    }
    if (last < first) {
      throw new IllegalArgumentException("Last position cannot be less than the first.");
    }
    this.first = first;
    this.last = last;
  }

  /**
   * @return The number of bytes contained in the range.
   */
  public int length() {
    return last - first + 1;
  }

  /**
   * @return A string formatted for use in Content-Range response headers.
   */
  public String toString() {
    return String.format("bytes %d-%d", first, last);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    HttpRequestByteRange that = (HttpRequestByteRange) o;
    return first == that.first &&
           last == that.last;
  }

  @Override
  public int hashCode() {
    return Objects.hash(first, last);
  }
}
