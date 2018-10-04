package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.request.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TEMP: This is naive in that it's unbounded and memory-based, meaning that after enough requests
 * it will cause the server to crash due to OOM. To be "production ready", this would at the very least
 * read/write to a file instead with the `loggedRequests` method being replaced by a sort of `tail`
 * method that returns the last N logs.
 */
public class AccessLogger {
  private final List<RequestLog> logs;

  public AccessLogger() {
    this.logs = Collections.synchronizedList(new ArrayList<>());
  }

  public void log(Request r) {
    // a real access log would track when reqs were received, but that will make cob_spec upset
    logs.add(new RequestLog(r.version, r.method, r.uri, r.headers));
  }

  public List<RequestLog> loggedRequests() {
    return List.copyOf(logs);
  }
}
