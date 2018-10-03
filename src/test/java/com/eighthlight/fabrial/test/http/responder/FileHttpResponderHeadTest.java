package com.eighthlight.fabrial.test.http.responder;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
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
    child.type = "text/plain";


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
    child.type = "text/plain";

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", Integer.toString(child.data.length)));
    assertThat(response.headers, hasEntry("Content-Type", child.type));
    assertThat(response.headers, hasEntry("Accept-Ranges", "bytes=0-" + (child.data.length - 1)));
  }

  @Test
  void headNestedFileWithData() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var childDir = new MockDirectory("fuz");
    mockFC.root.children.add(childDir);

    var grandChild = new MockFile("bar");
    childDir.children.add(grandChild);
    grandChild.data = "bytes".getBytes();
    grandChild.type = "image/jpeg";

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString(childDir.name + "/" + grandChild.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", Integer.toString(grandChild.data.length)));
    assertThat(response.headers, hasEntry("Content-Type", grandChild.type));
  }
}
