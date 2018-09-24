package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class FileHttpResponderGetTest {
  @Test
  void getEmptyFile() throws IOException {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var child = new MockFile("bar");
    mockFC.root.children.add(child);


    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.GET)
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", "0"));
    assertThat(response.body, is(nullValue()));
  }

  @Test
  void getFileWithData() throws IOException {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var child = new MockFile("bar");
    mockFC.root.children.add(child);
    child.data = "foo".getBytes();
    child.type = "text/plain";

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.GET)
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", Integer.toString(child.data.length)));
    assertThat(response.headers, hasEntry("Content-Type", child.type));
    assertThat(response.headers, hasEntry("Content-Range", "bytes=0-" + child.data.length));
    assertThat(response.body, is(not(nullValue())));
    assertThat(response.body.readAllBytes(), is(child.data));
  }
  @Test
  void getFirstByteOfFile() throws IOException {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var child = new MockFile("bar");
    mockFC.root.children.add(child);
    child.data = "foo".getBytes();
    child.type = "text/plain";

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.GET)
            .withHeaders(Map.of("Range", "bytes=0-0"))
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, hasEntry("Content-Length", Integer.toString(child.data.length)));
    assertThat(response.headers, hasEntry("Content-Type", child.type));
    assertThat(response.headers, hasEntry("Content-Range", "bytes=0-0/" + child.data.length));
    assertThat(response.body, is(not(nullValue())));
    assertThat(response.body.readAllBytes(), is(child.data));
  }
}
