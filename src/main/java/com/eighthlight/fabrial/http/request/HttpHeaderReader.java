package com.eighthlight.fabrial.http.request;

import java.io.InputStream;

public class HttpHeaderReader {
  private final InputStream is;

  public HttpHeaderReader(InputStream is) {
    this.is = is;
  }

  public String getKey() {
    return null;
  }

  public String getValue() {
    return null;
  }
}
