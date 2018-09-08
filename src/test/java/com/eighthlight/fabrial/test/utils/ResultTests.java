package com.eighthlight.fabrial.test.utils;

import com.eighthlight.fabrial.utils.Result;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ResultTests {
  @Test
  void wrapsValues() {
    assertThat(
        Result.attempt(() -> "foo").orElseAssert(),
        is("foo")
    );
  }

  @Test
  void wrapsExceptions() {
    assertThrows(RuntimeException.class, () ->
        Result
            .attempt(() -> {
              throw new RuntimeException();
            })
            .orElseThrow()
    );
  }

  @Test
  void wrapsSubtypeExceptions() {
    assertThrows(RuntimeException.class, () ->
        Result
            .<ObjectUtils.Null, Exception>attempt(() -> {
              throw new RuntimeException();
            })
            .orElseThrow()
    );
  }
}
