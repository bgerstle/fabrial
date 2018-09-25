package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.HttpRequestByteRange;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpRequestByteRangeTest {
  @Test
  void failsToParseEmptyString() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("", 1));
  }

  @Test
  void failsToParseUnitsOtherThanBytes() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bits=0-0", 1));
  }

  @Test
  void failsToParseEmptyRange() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=-", 1));
  }

  @Test
  void failsToParseNegativeFirstIndex() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=-1-", 1));
  }

  @Test
  void failsToParseFirstIndexWhichExceedsSize() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=2-", 1));
  }

  @Test
  void failsToParseNegativeLastIndex() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=0--1", 1));
  }

  @Test
  void failsToParseLastIndexWhichExceedsSize() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=0-2", 1));
  }

  @Test
  void failsToParseFirstIndexGreaterThanLastIndex() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=1-0", 1));
  }

  @Test
  void failsToParseRangeLargerThanSize() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("bytes=0-1", 1));
  }

  @Test
  void failsToParseEmptySuffixRange() {
    assertThrows(HttpRequestByteRange.ParsingException.class,
                 () -> HttpRequestByteRange.parseFromHeader("-0", 1));
  }

  @Test
  void parsesRangeWithFirstAndLast() throws HttpRequestByteRange.ParsingException {
    var range = HttpRequestByteRange.parseFromHeader("bytes=0-1", 2);
    assertThat(range.first, is(0));
    assertThat(range.last, is(1));
    assertThat(range.length(), is(2));
  }

  @Test
  void parsesSingleByteRange() throws HttpRequestByteRange.ParsingException {
    var range = HttpRequestByteRange.parseFromHeader("bytes=0-0", 2);
    assertThat(range.first, is(0));
    assertThat(range.last, is(0));
    assertThat(range.length(), is(1));
  }

  @Test
  void parsesRangeWithoutFirstIndex() throws HttpRequestByteRange.ParsingException {
    var range = HttpRequestByteRange.parseFromHeader("bytes=-2", 5);
    assertThat(range.first, is(3));
    assertThat(range.last, is(4));
    assertThat(range.length(), is(2));
  }

  @Test
  void parsesRangeWithoutLastIndex() throws HttpRequestByteRange.ParsingException {
    var range = HttpRequestByteRange.parseFromHeader("bytes=2-", 5);
    assertThat(range.first, is(2));
    assertThat(range.last, is(4));
    assertThat(range.length(), is(3));
  }

  @Test
  void throwsWhenInitializedWithInvalidFirstPosition() {
    assertThrows(IllegalArgumentException.class, () -> {
      new HttpRequestByteRange(-1, 0);
    });
  }

  @Test
  void throwsWhenFirstGreaterThanLast() {
    assertThrows(IllegalArgumentException.class, () -> {
      new HttpRequestByteRange(1, 0);
    });
  }

  @Test
  void formatsFirstAndLastInRange() {
    assertThat(new HttpRequestByteRange(0, 1).toString(), is("bytes 0-1"));
  }
}
