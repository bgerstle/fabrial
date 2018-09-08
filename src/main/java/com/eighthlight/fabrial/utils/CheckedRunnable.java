package com.eighthlight.fabrial.utils;

public interface CheckedRunnable<E extends Throwable> {
  void run() throws E;
}
