package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.request.Request;

import java.util.ArrayList;
import java.util.List;

public class AccessLogger {
  private final ArrayList<RequestLog> logs;

  public AccessLogger() {
    this.logs = new ArrayList<>();
  }

  public void log(Request r) {
    logs.add(new RequestLog(r.version, r.method, r.uri, r.headers));
  }

  public List<RequestLog> loggedRequests() {
    return List.copyOf(logs);
  }
}
