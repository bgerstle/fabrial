package com.eighthlight.fabrial.http.message;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMessageBuilder<T extends AbstractMessageBuilder<T, B>, B>
    implements HttpMessageBuilder<T, B> {
  protected String version;
  protected InputStream body;
  protected Map<String, String> headers;

  public AbstractMessageBuilder() {
    headers = new HashMap<>();
  }

  // Set version from a string with the format "HTTP/X.Y"
  public T withPrefixedVersion(String prefixedVersion) {
    String[] versionComponents = prefixedVersion.split("/");
    if (versionComponents.length < 2) {
      throw new IllegalArgumentException("Expected 'HTTP/X.Y', got: " + prefixedVersion);
    }
    return (T)withVersion(versionComponents[1]);
  }

  public T withVersion(String version) {
    this.version = version;
    return (T)this;
  }

  @Override
  public T withHeaders(Map<String, String> headers) {
    this.headers = Optional.ofNullable(headers).map(HashMap::new).orElse(null);
    return (T)this;
  }

  @Override
  public T withBody(InputStream body) {
    this.body = body;
    return (T)this;
  }
}
