package com.eighthlight.fabrial;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogConfig {
  public static void apply() {
    Logger rootLogger = Logger.getGlobal().getParent();
    for (Handler handler: rootLogger.getHandlers()) {
      handler.setFormatter(new Formatter() {
        @Override
        public String format(LogRecord record) {
          return String.format("%1$tFT%1$tT.%1$tL [%2$s::%3$s|tid:%4$s|%5$s] %6$s %7$s %n",
                               LocalDateTime.ofInstant(record.getInstant(), ZoneOffset.UTC),
                               ClassUtils.getShortClassName(record.getSourceClassName()),
                               record.getSourceMethodName(),
                               record.getThreadID(),
                               record.getLevel(),
                               record.getMessage(),
                               Optional.ofNullable(record.getThrown())
                                       .map(ExceptionUtils::getStackTrace)
                                       .orElse(""));

        }
      });
    }
  }
}
