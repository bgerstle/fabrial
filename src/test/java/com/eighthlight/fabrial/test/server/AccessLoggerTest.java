package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.AccessLogger;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.RequestLog;
import com.eighthlight.fabrial.http.message.request.Request;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccessLoggerTest {
  @Test
  void noLogsForNoRequests() {
    assertThat(new AccessLogger().loggedRequests(), equalTo(List.of()));
  }

  @Test
  void returnsLoggedRequests() throws Exception {
    var accessLogger = new AccessLogger();
    var requests = List.of(
        new Request(HttpVersion.ONE_ONE, Method.GET, new URI("/")),
        new Request(HttpVersion.TWO_ZERO,
                    Method.PUT,
                    new URI("/foo"),
                    Map.of("foo", "bar"),
                    null));
    requests.forEach(accessLogger::log);
    assertThat(accessLogger.loggedRequests(),
               equalTo(requests.stream()
                               .map(r -> new RequestLog(r.version, r.method, r.uri, r.headers))
                               .collect(Collectors.toList())));
  }
}
