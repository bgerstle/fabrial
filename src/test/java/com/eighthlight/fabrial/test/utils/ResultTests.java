package com.eighthlight.fabrial.test.utils;

import com.eighthlight.fabrial.utils.Result;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.oneOf;
import static org.quicktheories.generators.SourceDSL.strings;

public class ResultTests {
  public static Gen<? extends Result<Object, Exception>> results() {
    Gen<String> strs =  strings().allPossible().ofLengthBetween(1, 5);
    return
        oneOf(strs.map(s -> (Object)s),
              strs.map(s -> new Exception(s)).map(e -> (Object)e))
            .map(Result::of);
  }


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
        Result.<ObjectUtils.Null, Exception>attempt(() -> {
          throw new RuntimeException();
        })
            .orElseThrow()
    );
  }

  @Test
  void flatmapsNewValue() {
    assertThat(
        Result.attempt(() -> "foo")
              .flatMap((f) -> Result.success(f + "bar"))
              .orElseAssert(),
        is("foobar")
    );
  }

  @Test
  void flatmapsNewError() {
    assertThrows(StringIndexOutOfBoundsException.class, () ->
        Result.success("foo")
              .flatMapAttempt((f) -> f.charAt(f.length()))
              .orElseThrow()
    );
  }

  @Test
  void errorsSuppressSubsequentMaps() {
    assertThrows(StringIndexOutOfBoundsException.class, () ->
        Result.success("foo")
              .flatMapAttempt((f) -> f.charAt(f.length()))
              .map((c) -> {
                fail();
                return Character.toUpperCase(c);
              })
              .orElseThrow()
    );
  }

  @Test
  void errorResultsAreEmpty() {
    assertThat(Result.failure(new Exception()).toOptional().isPresent(), is(false));
  }

  @Test
  void successResultsArePresent() {
    assertThat(Result.success(0).toOptional().isPresent(), is(true));
  }

  @Test
  void resultEquality() {
    qt().forAll(results(), results().toOptionals(50))
        .checkAssert((r1, r2) -> {
          assertThat(
              // all result equality-based method results
              (r1.equals(r2.orElse(null))
               && r1.toString().equals(r2.map(Result::toString).orElse(null))
               && r1.hashCode() == r2.map(Result::hashCode).orElse(null)),
              // are identical to the equality of their underlying value/error
              is(r1.getEither().equals(r2.map(Result::getEither).orElse(null))));
        });
  }

  @Test
  void resultIdentity() {
    qt().forAll(results())
        .checkAssert((r) -> {
          assertThat(
              (r.equals(r)
               && r.toString().equals(r.toString())
               && r.hashCode() == r.hashCode()),
              is(true));
        });
  }
}
