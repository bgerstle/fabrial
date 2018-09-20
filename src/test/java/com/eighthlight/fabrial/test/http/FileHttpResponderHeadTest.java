package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

public class FileHttpResponderHeadTest {
  @Test
  void headAbsentFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString("baz")
            .build());
    assertThat(response.statusCode, is(404));
    assertThat(response.headers, is(emptyMap()));
  }

  @Test
  void headRootDir() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString("/")
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", "0"));
  }

  @Test
  void headEmptyFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var child = new MockFile("bar");
    mockFC.root.children.add(child);


    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", "0"));
  }

  @Test
  void headFileWithData() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var child = new MockFile("bar");
    mockFC.root.children.add(child);
    child.data = "bytes".getBytes();

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", Integer.toString(child.data.length)));
  }
}
