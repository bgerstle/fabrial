package com.eighthlight.fabrial.test.http.responder;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

public class FileHttpResponderDeleteTest {
  @Test
  void deleteFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var file = new MockFile("bar");
    mockFC.root.children.add(file);

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.DELETE)
            .withUriString(file.getName())
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, is(emptyMap()));

    assertThat(mockFC.root.children, not(contains(file)));
  }

  @Test
  void deleteAbsentFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.DELETE)
            .withUriString("bar")
            .build());
    assertThat(response.statusCode, is(404));
    assertThat(response.headers, is(emptyMap()));
  }

  @Test
  void deleteDirWithFiles() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var childDir = new MockDirectory("bar");
    mockFC.root.children.add(childDir);

    childDir.children.add(new MockFile("baz"));

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.DELETE)
            .withUriString("bar")
            .build());
    assertThat(response.statusCode, is(500));
    assertThat(response.headers, is(emptyMap()));
  }
}
