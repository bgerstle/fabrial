package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.Response;
import com.eighthlight.fabrial.server.HttpConnectionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class HttpConnectionHandlerTests {
  HttpConnectionHandler handler = new HttpConnectionHandler();

  String sendRequest(Request req) throws Throwable {
    ByteArrayOutputStream reqOs = new ByteArrayOutputStream();
    try {
      req.writeTo(reqOs);
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize test request", e);
    }
    ByteArrayInputStream is = new ByteArrayInputStream(reqOs.toByteArray());
    ByteArrayOutputStream respOs = new ByteArrayOutputStream();
    handler.handle(is, respOs);
    return new String(respOs.toByteArray(), StandardCharsets.UTF_8);
  }


  @Test
  void responds200ToTestHeadRequest() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(sendRequest(new Request(version, Method.HEAD, new URI("/test"))),
               equalTo("HTTP/" + version + " 200 \r\n"));
  }

  @Test
  void responds501ToUnimplementedMethods() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(sendRequest(new Request(version, Method.DELETE, new URI("/"))),
               equalTo("HTTP/" + version + " 200 \r\n"));
  }

  @Test
  void responds404ToNonTestPaths() throws Throwable {
    String version = HttpVersion.ONE_ONE;
    assertThat(sendRequest(new Request(version, Method.HEAD, new URI("/foobarbazbuz"))),
               equalTo("HTTP/" + version + " 404 \r\n"));
  }

  @Test
  void responds501ToUnsupportedHttpVersions() throws Throwable {
    String version = HttpVersion.ZERO_NINE;
    assertThat(sendRequest(new Request(version, Method.HEAD, new URI("/test"))),
               allOf(
                   startsWith("HTTP/" + HttpVersion.ONE_ONE + " 501 "),
                   endsWith("\r\n")
               ));
  }
}
