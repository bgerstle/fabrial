package com.eighthlight.fabrial.http.message;

import java.io.InputStream;
import java.util.Map;

public interface HttpMessageBuilder<T extends HttpMessageBuilder<T, B>, B> {
  T withHeaders(Map<String, String>headers);
  T withBody(InputStream body);
  B build();
}
