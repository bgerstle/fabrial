package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.AccessLogger;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.RequestLog;
import com.eighthlight.fabrial.http.message.request.Request;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.requests;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.lists;

public class AccessLoggerTest {
  @Test
  void noLogsForNoRequests() {
    assertThat(new AccessLogger().loggedRequests(), equalTo(List.of()));
  }

  @Test
  void returnsLoggedRequests() throws Exception {
    var accessLogger = new AccessLogger();
    var requests = List.of(
        new Request(HttpVersion.ONE_ONE, Method.GET.name(), new URI("/")),
        new Request(HttpVersion.TWO_ZERO,
                    Method.PUT.name(),
                    new URI("/foo"),
                    Map.of("foo", "bar"),
                    null));
    requests.forEach(accessLogger::log);
    assertThat(accessLogger.loggedRequests(),
               equalTo(requests.stream()
                               .map(r -> new RequestLog(r.version, r.method, r.uri, r.headers))
                               .collect(Collectors.toList())));
  }

  @Test
  void logsArbitraryRequests() {
    qt().forAll(lists().of(requests()).ofSizeBetween(1, 100))
        .checkAssert(rs -> {
          var accessLogger = new AccessLogger();
          rs.forEach(accessLogger::log);
          assertThat(accessLogger.loggedRequests(),
                     containsInAnyOrder(rs.stream()
                                          .map(r -> new RequestLog(r.version, r.method, r.uri, r.headers))
                                          .map(CoreMatchers::equalTo)
                                          .collect(Collectors.toList())));
        });
  }

  @Test
  void returnsConcurrentlyLoggedRequests() throws Exception {
    // just using qt as a way to generate arbitrary lists of inputs for quantity, no shrinking
    qt().withExamples(5)
        .withShrinkCycles(0)
        .forAll(lists().of(requests()).ofSizeBetween(100, 1000))
        .checkAssert(rs -> {
          var accessLogger = new AccessLogger();
          rs.parallelStream()
            .forEach(accessLogger::log);
          assertThat(accessLogger.loggedRequests(),
                     containsInAnyOrder(rs.stream()
                                                .map(r -> new RequestLog(r.version, r.method, r.uri, r.headers))
                                                .map(CoreMatchers::equalTo)
                                                .collect(Collectors.toList())));
        });
  }
}
