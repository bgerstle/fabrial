package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.request.HttpRequestByteRange;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.api.Pair;
import org.quicktheories.api.Tuple3;
import org.quicktheories.core.Gen;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;

public class ArbitraryByteRangeTest {
  private static Gen<Pair<Integer, Integer>> fileSizeAndPosition() {
    return integers().from(1)
                     .upToAndIncluding(Integer.MAX_VALUE - 1)
                     .mutate((size, rand) -> {
                       return Pair.of(size, integers().between(0, size - 1).generate(rand));
                     });
  }
  private static Gen<Tuple3<Integer, Integer, Integer>> fileSizeFirstPosAndLastPos() {
    return fileSizeAndPosition().mutate((sizeAndFirst, rand) -> {
      var size = sizeAndFirst._1;
      var first = sizeAndFirst._2;
      return Tuple3.of(size, first, integers().between(first, size - 1).generate(rand));
    });
  }

  @Test
  void parsesAllValidRangesWithFirstAndLastPositions() {
    qt().forAll(fileSizeFirstPosAndLastPos())
        .checkAssert(t3 -> {
          var size = t3._1;
          var first = t3._2;
          var last = t3._3;
          var rangeHeader = String.format("bytes=%d-%d", first, last);
          var range = Result
              .attempt(() -> HttpRequestByteRange.parseFromHeader(rangeHeader, size))
              .orElseAssert();
          assertThat(range.first, is(first));
          assertThat(range.last, is(last));
          assertThat(range.length(), is(last - first + 1));
        });
  }

  @Test
  void failsToParseAllRangesWhereFirstGreaterThanLast() {
    qt().forAll(fileSizeFirstPosAndLastPos())
        // except ranges where first & last are equal, since they're equivalent to their inversions
        .assuming(t3 -> !t3._2.equals(t3._3))
        .checkAssert(t3 -> {
          var size = t3._1;
          var first = t3._2;
          var last = t3._3;
          // swap last & first, resulting in an invalid range
          var rangeHeader = String.format("bytes=%d-%d", last, first);
          assertThrows(HttpRequestByteRange.ParsingException.class,
                       () -> HttpRequestByteRange.parseFromHeader(rangeHeader, size));
        });
  }

  @Test
  void parsesRangesWithValidFirstPosition() {
    qt().forAll(fileSizeAndPosition())
        .checkAssert(t2 -> {
          var size = t2._1;
          var first = t2._2;
          var rangeHeader = String.format("bytes=%d-", first);
          var range = Result
              .attempt(() -> HttpRequestByteRange.parseFromHeader(rangeHeader, size))
              .orElseAssert();
          assertThat(range.first, is(first));
          assertThat(range.last, is(size - 1));
          assertThat(range.length(), is(size - first));
        });
  }

  @Test
  void parsesRangesWithValidLastPosition() {
    qt().forAll(fileSizeAndPosition())
        .checkAssert(t2 -> {
          var size = t2._1;
          var suffixLength = t2._2;
          var rangeHeader = String.format("bytes=-%d", suffixLength);
          var range = Result
              .attempt(() -> HttpRequestByteRange.parseFromHeader(rangeHeader, size))
              .orElseAssert();
          assertThat(range.first, is(size - suffixLength));
          assertThat(range.last, is(size - 1));
          assertThat(range.length(), is(suffixLength));
        });
  }

  @Test
  void rangesWithTheSameFirstAndLastPositionsAreEqual() {
    qt().forAll(fileSizeFirstPosAndLastPos(), fileSizeFirstPosAndLastPos())
        .checkAssert((t1, t2) -> {
          var r1 = new HttpRequestByteRange(t1._2, t1._3);
          var r2 = new HttpRequestByteRange(t2._2, t2._3);
          assertThat(r1.equals(r2), equalTo(
              r1.first == r2.first
              && r1.last == r2.last
          ));
          assertThat(r1.equals(r2), equalTo(r1.hashCode() == r2.hashCode()));
        });
  }

  @Test
  void identicallyConstructedRangesAreEqual() {
    qt().forAll(fileSizeFirstPosAndLastPos())
        .checkAssert((t1) -> {
          var r1 = new HttpRequestByteRange(t1._2, t1._3);
          var r2 = new HttpRequestByteRange(t1._2, t1._3);
          assertThat(r1, equalTo(r2));
          assertThat(r1.hashCode(), equalTo(r2.hashCode()));
        });
  }
}
