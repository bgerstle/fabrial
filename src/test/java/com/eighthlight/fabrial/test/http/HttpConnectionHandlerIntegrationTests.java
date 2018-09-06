package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.server.HttpConnectionHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class HttpConnectionHandlerIntegrationTests {
  HttpConnectionHandler handler = new HttpConnectionHandler();

  String sendRequest(Request req) throws Throwable {
    ByteArrayOutputStream reqOs = new ByteArrayOutputStream();
    try {
      req.writeTo(reqOs);
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
        sendRequest(Request.builder()
                           .withVersion (version)
                           .withMethod(Method.HEAD)
                           .withUriString("/test").build()),
        equalTo("HTTP/" + version + " 200 \r\n"));
  }

  @Test
  void responds501ToUnimplementedMethods() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(
        sendRequest(Request.builder()
                           .withVersion (version)
                           .withMethod(Method.DELETE)
                           .withUriString("/")
                           .build()),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 501 "),
            endsWith("\r\n")
        ));
  }

  @Test
  void responds404ToNonTestPaths() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(
        sendRequest(Request.builder()
                           .withVersion (version)
                           .withMethod(Method.HEAD)
                           .withUriString("/foobarbuz")
                           .build()),
        equalTo("HTTP/" + version + " 404 \r\n"));
  }

  @Test
  void responds501ToUnsupportedHttpVersions() throws Throwable {
    String version = HttpVersion.ZERO_NINE;
    assertThat(
        sendRequest(Request.builder()
                           .withVersion (version)
                           .withMethod(Method.HEAD)
                           .withUriString("/test")
                           .build()),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 501 "),
            endsWith("\r\n")
        ));
  }

  @Test
  void handlesMalformedRequests() {

    assertThat(
        sendRequest("FOO"),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 400 "),
            endsWith("\r\n")
        ));
  }
}
