package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.request.Request;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
