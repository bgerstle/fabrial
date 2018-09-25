package com.eighthlight.fabrial.http.request;

import com.eighthlight.fabrial.utils.Result;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

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

  private static final Pattern RANGE_PATTERN = Pattern.compile("([^=]+)=(\\d*)-(\\d*)");

  public static HttpRequestByteRange parseFromHeader(
      String headerValue,
      int fileSize) throws ParsingException {
    var matcher = RANGE_PATTERN.matcher(headerValue);
    if (!matcher.find()) {
      throw new ParsingException("Failed to match range components. Expected 'bytes=first?-last?'");
    }

    var unit = matcher.group(1);
    if (!"bytes".equals(unit)) {
      throw new ParsingException("Unacceptable range request unit: " + unit);
    }

    var first = Result.<String, NumberFormatException>of(
        Optional
            .ofNullable(matcher.group(2))
            .flatMap(s -> s.isEmpty() ? Optional.empty() : Optional.of(s))
            .orElse("0")
    ).flatMapAttempt(Integer::parseInt)
     .flatMap(i -> i < 0 || i > fileSize - 1 ? Result.failure(new ParsingException("Out of bounds first position")) : Result.success(i))
     .orElseThrow();

    var last = Result.<String, NumberFormatException>of(
        Optional
            .ofNullable(matcher.group(3))
            .flatMap(s -> s.isEmpty() ? Optional.empty() : Optional.of(s))
            .orElse(Integer.toString(fileSize - 1))
    ).flatMapAttempt(Integer::parseInt)
     .flatMap(i -> i < 0 || i > fileSize - 1 ? Result.failure(new ParsingException("Out of bounds last position")) : Result.success(i))
     .orElseThrow();

    return new HttpRequestByteRange(first, last);
  }

  public HttpRequestByteRange(int first, int last) {
    this.first = first;
    this.last = last;
  }

  public int length() {
    return last - first + 1;
  }

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
