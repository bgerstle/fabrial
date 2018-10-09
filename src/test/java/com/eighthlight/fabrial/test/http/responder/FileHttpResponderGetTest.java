package com.eighthlight.fabrial.test.http.responder;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
    assertThat(response.headers, not(hasKey("Content-Range")));

    assertThat(response.body, is(not(nullValue())));
    assertThat(response.body.readAllBytes(), is(child.data));
  }

  @Test
  void getFirstTwoBytesOfFile() throws IOException {
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
            .withHeaders(Map.of("Range", "bytes=0-1"))
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(206));
    assertThat(response.headers, hasEntry("Content-Length", "2"));
    assertThat(response.headers, hasEntry("Content-Type", child.type));
    assertThat(response.headers, hasEntry("Content-Range", "bytes 0-1/" + child.data.length));
    assertThat(response.body, is(not(nullValue())));
    assertThat(new String(response.body.readAllBytes()),
               is(new String(Arrays.copyOfRange(child.data, 0, 2))));
  }

  @Test
  void sends500ResponseOnReadError() throws IOException {
    var mockFC = new MockFileController() {
      @Override
      public InputStream getFileContents(String relPathStr, int offset, int length) throws IOException {
        throw new IOException("test");
      }
    };
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    // file must have data, otherwise we'll either get a 200 for an empty file response
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
    assertThat(response.statusCode, is(500));
  }

  @Test
  void respondsToInvalidToFromRangeWith416() {
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
            .withHeaders(Map.of("Range", "bytes=10-0"))
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(416));
    assertThat(response.reason, containsString("Last position cannot be less than the first"));
    assertThat(response.headers, hasEntry("Content-Range", "bytes */" + child.data.length));
  }

  @Test
  void respondsToSuffixLongerThanFileWithEntireFile() throws Exception {
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
            .withHeaders(Map.of("Range", "bytes=-" + (child.data.length + 1)))
            .withUriString(child.name)
            .build());
    assertThat(response.statusCode, is(206));
    assertThat(response.headers, hasEntry("Content-Length", Integer.toString(child.data.length)));
    assertThat(response.headers, hasEntry("Content-Type", child.type));
    assertThat(response.headers, hasEntry("Content-Range", "bytes 0-" + (child.data.length - 2) + "/" + child.data.length));
    assertThat(response.body, is(not(nullValue())));
    assertThat(new String(response.body.readAllBytes()),
               is(new String(child.data)));
  }
}
