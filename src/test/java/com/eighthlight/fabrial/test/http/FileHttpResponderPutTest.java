package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileHttpResponderPutTest {
  @Test
  void putAbsentFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.PUT)
            .withUriString("baz")
            .build());
    assertThat(response.statusCode, is(404));
    assertThat(response.headers, is(emptyMap()));
  }

  @Test
  void putWithoutContentLengthHeader() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var data = "buz".getBytes();

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.PUT)
            .withUriString("baz")
            .withBody(new ByteArrayInputStream(data))
            .build());
    assertThat(response.statusCode, is(411));
    assertThat(response.headers, is(emptyMap()));
  }
}
