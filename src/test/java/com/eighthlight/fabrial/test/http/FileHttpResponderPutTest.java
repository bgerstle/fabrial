package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileHttpResponderPutTest {
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

  @Test
  void putWithInvalidContentLengthHeader() {
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
            .withHeaders(Map.of(
                "Content-Length", "fuz"
            ))
            .build());
    assertThat(response.statusCode, is(411));
    assertThat(response.headers, is(emptyMap()));
  }

  @Test
  void putCreateFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var createdFilename = "baz";

    var data = "buz".getBytes();

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.PUT)
            .withUriString(createdFilename)
            .withBody(new ByteArrayInputStream(data))
            .withHeaders(Map.of(
                "Content-Length", Integer.toString(data.length)
            ))
            .build());
    assertThat(response.statusCode, is(201));
    assertThat(response.headers, is(emptyMap()));

    var fileData =
        Result.attempt(() -> mockFC.getFileContents(createdFilename, 0, data.length))
              .flatMapAttempt(bais -> bais.readAllBytes())
              .orElseAssert();
    assertThat(fileData, is(data));
  }

  @Test
  void putModifyFile() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var file = new MockFile("bar");
    mockFC.root.children.add(file);

    var data = "buz".getBytes();

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.PUT)
            .withUriString(file.getName())
            .withBody(new ByteArrayInputStream(data))
            .withHeaders(Map.of(
                "Content-Length", Integer.toString(data.length)
            ))
            .build());
    assertThat(response.statusCode, is(200));
    assertThat(response.headers, is(emptyMap()));

    assertThat(file.data, is(data));
  }


  @Test
  void updatingRootIsBadRequest() {
    var mockFC = new MockFileController();
    var responder = new FileHttpResponder(mockFC);

    mockFC.root = new MockDirectory("foo");

    var data = "buz".getBytes();

    var response = responder.getResponse(
        new RequestBuilder()
            .withVersion(HttpVersion.ONE_ONE)
            .withMethod(Method.PUT)
            .withUriString("/")
            .withBody(new ByteArrayInputStream(data))
            .withHeaders(Map.of(
                "Content-Length", Integer.toString(data.length)
            ))
            .build());
    assertThat(response.statusCode, is(400));
    assertThat(response.headers, is(emptyMap()));
  }
}
