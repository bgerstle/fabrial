package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.*;
import com.eighthlight.fabrial.server.HttpConnectionHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class HttpConnectionHandlerIOStreamTests implements HttpResponder {
  @Override
  public boolean matches(Request request) {
    return request.uri.getPath().equals("/test");
  }

  @Override
  public Response getResponse(Request request) {
    if (!request.method.equals(Method.HEAD)) {
      return new Response(HttpVersion.ONE_ONE, 501, null);
    }
    return new Response(HttpVersion.ONE_ONE, 200, null);
  }

  HttpConnectionHandler handler = new HttpConnectionHandler(Set.of(this));

  String sendRequest(Request req) throws Throwable {
    ByteArrayOutputStream reqOs = new ByteArrayOutputStream();
    try {
      new RequestWriter(reqOs).writeRequest(req);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize test request", e);
    }
    return sendRequest(reqOs.toByteArray());
  }

  String sendRequest(String request) {
    return sendRequest(StandardCharsets.UTF_8.encode(request).array());
  }

  String sendRequest(byte[] request) {
    ByteArrayInputStream is = new ByteArrayInputStream(request);
    ByteArrayOutputStream respOs = new ByteArrayOutputStream();
    try {
      handler.handle(is, respOs);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    return new String(respOs.toByteArray(), StandardCharsets.UTF_8);
  }


  @Test
  void responds200ToTestHeadRequest() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(
        sendRequest(new RequestBuilder()
                           .withVersion (version)
                           .withMethod(Method.HEAD)
                           .withUriString("/test").build()),
        equalTo("HTTP/" + version + " 200 " + CRLF));
  }

  @Test
  void responds501ToUnimplementedMethods() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(
        sendRequest(new RequestBuilder()
                           .withVersion (version)
                           .withMethod(Method.DELETE)
                           .withUriString("/test")
                           .build()),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 501 "),
            endsWith(CRLF)
        ));
  }

  @Test
  void responds404ToNonTestPaths() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(
        sendRequest(new RequestBuilder()
                           .withVersion (version)
                           .withMethod(Method.HEAD)
                           .withUriString("/foobarbuz")
                           .build()),
        equalTo("HTTP/" + version + " 404 " + CRLF));
  }

  @Test
  void responds501ToUnsupportedHttpVersions() throws Throwable {
    String version = HttpVersion.ZERO_NINE;
    assertThat(
        sendRequest(new RequestBuilder()
                           .withVersion (version)
                           .withMethod(Method.HEAD)
                           .withUriString("/test")
                           .build()),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 501 "),
            endsWith(CRLF)
        ));
  }

  @Test
  void handlesMalformedRequests() {
    assertThat(
        sendRequest("FOO"),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 400 "),
            endsWith(CRLF)
        ));
  }
}
