package com.eighthlight.fabrial.utils;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class Result<V, E extends Throwable> {
  private final V value;
  private final E error;

  private Result(V value, E error) {
    this.value = value;
    this.error = error;
  }

  public Optional<V> getValue() {
    return Optional.ofNullable(value);
  }

  public Optional<E> getError() {
    return Optional.ofNullable(error);
  }

  public static <V, E extends Throwable> Result<V, E> failure(E error) {;
    return new Result<>(null, Objects.requireNonNull(error));
  }

  public static <V, E extends Throwable> Result<V, E> success(V value) {
    return new Result<>(Objects.requireNonNull(value), null);
  }

  public static <E extends Throwable>
  Result<ObjectUtils.Null, E> attempt(CheckedRunnable<? extends E> r) {
    return attempt(() -> {
      r.run();
      return ObjectUtils.NULL;
    });
  }

  public static <V, E extends Throwable>
  Result<V, E> attempt(CheckedProvider<? extends V, ? extends E> p) {
    try {
      return Result.success(p.get());
    } catch (Throwable e) {
      return Result.failure((E)e);
    }
  }

  public <T> Result<T, E> map(Function<? super V, ? extends T> mapper) {
    return Optional.ofNullable(error)
                   // Result.failure type must be specified,
                   // or type of fallback below can't be determined
                   .map(e -> Result.<T, E>failure(e))
                   .orElseGet(() -> Result.success(mapper.apply(value)));
  }

  public <T> Result<T, E> flatMapAttempt(CheckedFunction<? super V, ? extends T, ? super E> cf) {
    return getValue()
        .map((v) -> (Result<T, E>)attempt(() -> cf.apply(v)))
        .orElseGet(() -> Result.failure(Objects.requireNonNull(error)));
  }

  public <T> Result<T, E> flatMap(Function<? super V, ? extends Result<? extends T, ? super E>> mapper) {
    return getValue()
        .map((v) ->
          Objects.requireNonNull((Result<T, E>) mapper.apply(v))
        )
        .orElseGet(() ->
                       Result.failure(Objects.requireNonNull(error))
        );
  }

  public V orElseThrow() throws E {
    return Optional.ofNullable(value).orElseThrow(() -> error);
  }

  public V orElseAssert() {
    return Optional.ofNullable(value).orElseThrow(() -> new AssertionError(error));
  }

  public Optional<V> toOptional() {
    return Optional.ofNullable(value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Result<?, ?> result = (Result<?, ?>) o;
    return Objects.equals(value, result.value) &&
           Objects.equals(error, result.error);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, error);
  }

  @Override
  public String toString() {
    return "Result{" +
           "value=" + value +
           ", error=" + error +
           '}';
  }
}
