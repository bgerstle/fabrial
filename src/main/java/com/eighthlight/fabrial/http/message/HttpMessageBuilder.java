package com.eighthlight.fabrial.http.message;

import java.io.InputStream;
import java.util.Map;

/**
 * Common interface for building HTTP messages.
 * @param <Self> The type of builder (should always be self).
 * @param <MessageT>The type of message the builder constructs.
 */
public interface HttpMessageBuilder<Self extends HttpMessageBuilder<Self, MessageT>, MessageT> {
  Self withHeaders(Map<String, String>headers);
  Self withBody(InputStream body);
  MessageT build();
}
