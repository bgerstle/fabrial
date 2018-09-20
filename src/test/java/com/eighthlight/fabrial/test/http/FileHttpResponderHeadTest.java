package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;

public class FileHttpResponderHeadTest {
  @Test
  void headEmptyFile() {
    var mockFC = new MockFileController();
    mockFC.root = new MockDirectory("foo");

    var child = new MockFile("bar");
    mockFC.root.children.add(child);

    var responder = new FileHttpResponder(mockFC);

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.HEAD)
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", "0"));
  }
}
