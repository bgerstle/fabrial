package com.eighthlight.fabrial.utils;

@FunctionalInterface
public interface CheckedProvider<V, E extends Throwable> {
  V get() throws E;
}
