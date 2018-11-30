package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpConnectionHandler;
import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.test.http.request.RequestWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static com.eighthlight.fabrial.test.http.request.HttpRequestLineParsingTests.unspecifiedMethods;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.quicktheories.QuickTheory.qt;

public class HttpConnectionHandlerIOStreamTests implements HttpResponder {
  @Override
  public boolean matches(Request request) {
    return request.uri.getPath().equals("/test");
  }

  @Override
  public Response getResponse(Request request) {
    if (!request.method.equals(Method.HEAD.name())) {
      return new ResponseBuilder().withVersion(HttpVersion.ONE_ONE).withStatusCode(501).build();
    }
    return new ResponseBuilder().withVersion(HttpVersion.ONE_ONE).withStatusCode(200).build();
  }

  HttpConnectionHandler handler = new HttpConnectionHandler(List.of(this), null);

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
      handler.handleConnectionStreams(is, respOs);
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    return new String(respOs.toByteArray(), StandardCharsets.UTF_8);
  }

  @Test
  void handlesMalformedRequests() {
    assertThat(
        sendRequest("FOO"),
        allOf(
            startsWith("HTTP/" + HttpVersion.ONE_ONE + " 400 ")
        ));
  }
}
