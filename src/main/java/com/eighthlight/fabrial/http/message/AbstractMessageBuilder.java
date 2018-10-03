package com.eighthlight.fabrial.http.message;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractMessageBuilder<Self extends AbstractMessageBuilder<Self, MessageT>, MessageT>
    implements HttpMessageBuilder<Self, MessageT> {
  protected String version;
  protected InputStream body;
  protected Map<String, String> headers;

  public AbstractMessageBuilder() {
    headers = new HashMap<>();
  }

  // Set version from a string with the format "HTTP/X.Y"
  public Self withPrefixedVersion(String prefixedVersion) {
    String[] versionComponents = prefixedVersion.split("/");
    if (versionComponents.length < 2) {
      throw new IllegalArgumentException("Expected 'HTTP/X.Y', got: " + prefixedVersion);
    }
    return withVersion(versionComponents[1]);
  }

  public Self withVersion(String version) {
    this.version = version;
    @SuppressWarnings("unchecked")
    Self result = (Self)this;
    return result;
  }

  @Override
  public Self withHeaders(Map<String, String> headers) {
    this.headers = Optional.ofNullable(headers).map(HashMap::new).orElse(null);
    @SuppressWarnings("unchecked")
    Self result = (Self)this;
    return result;
  }

  @Override
  public Self withBody(InputStream body) {
    this.body = body;
    @SuppressWarnings("unchecked")
    Self result = (Self)this;
    return result;
  }
}
