package com.eighthlight.fabrial.test.file;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.file.LocalFilesystemController;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.test.fixtures.TempDirectoryFixture;
import com.eighthlight.fabrial.test.fixtures.TempFileFixture;
import com.bgerstle.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.paths;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.AllOf.allOf;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.pick;
import static org.quicktheories.generators.SourceDSL.strings;

public class FileHttpResponderIntegrationTest {
  @Test
  void headEmptyFile() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent()));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.HEAD)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, hasEntry("Content-Length", "0"));
    }
  }

  @Test
  void headAbsentFileNotFound() {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.HEAD)
              .withUriString("/foo")
              .build());
      assertThat(response.statusCode, is(404));
      assertThat(response.headers, is(anEmptyMap()));
      assertThat(response.body, is(nullValue()));
    }
  }

  @Test
  void headTextFile() {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt")) {
      var data = "foo".getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(data));
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent()));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.HEAD)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, hasEntry("Content-Length", Long.toString(data.length)));
      assertThat(response.headers, hasEntry("Content-Type", "text/plain"));
    }
  }

  @Test
  void unsupportedMethodOnExistingFile() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent()));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.POST)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .build());
      assertThat(response.statusCode, is(405));
      assertThat(response.headers, is(anEmptyMap()));
      assertThat(response.body, is(nullValue()));
    }
  }

  @Test
  void unsupportedMethodOnAbsentFile() {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.POST)
              .withUriString("/foo")
              .build());
      assertThat(response.statusCode, is(404));
      assertThat(response.headers, is(anEmptyMap()));
      assertThat(response.body, is(nullValue()));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"/", "/foo"})
  void optionsOnAbsentOrExistingFile(String path) {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.OPTIONS)
              .withUriString(path)
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, allOf(
          hasEntry(is("Allow"), allOf(containsString("GET"),
                                      containsString("DELETE"),
                                      containsString("PUT"),
                                      containsString("HEAD"),
                                      containsString("OPTIONS"))),
          hasEntry("Content-Length", "0")));
      assertThat(response.body, is(nullValue()));
    }
  }

  @Test
  void returnsAllFilesInDirWithMultipleFiles() throws IOException {
    try (var tmpDirFixture = new TempDirectoryFixture();
        var tmpFileFixture1 = new TempFileFixture(tmpDirFixture.tempDirPath);
        var tmpFileFixture2 = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString("/")
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, hasEntry("Content-Type", "text/html"));
      var baos = new ByteArrayOutputStream();
      response.body.transferTo(baos);
      var bodyString = baos.toString();
      assertThat(bodyString,
                 allOf(
                     containsString(tmpFileFixture1.tempFilePath.getFileName().toString()),
                     containsString(tmpFileFixture2.tempFilePath.getFileName().toString())));
      assertThat(response.headers, hasKey("Content-Length"));
    }
  }

  @Test
  void getEmptyDirectoryResponseBodyIsEmpty() {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString("/")
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, not(hasKey("Content-Type")));
      assertThat(response.headers, hasEntry("Content-Length", "0"));
      assertThat(response.body, is(nullValue()));
    }
  }

  @Test
  void getAbsentFileNotFound() {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString("/foo")
              .build());
      assertThat(response.statusCode, is(404));
      assertThat(response.headers, is(anEmptyMap()));
      assertThat(response.body, is(nullValue()));
    }
  }

  @Test
  void getEmptyFileReturnsEmpty200() {
    try (var tmpFileFixture = new TempFileFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent()));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, hasEntry("Content-Length", "0"));
      assertThat(response.body, is(nullValue()));
    }
  }

  @Test
  void getFileReturnsContents() {
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt")) {
      var data = "foo".getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(data));
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent()));

      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .build());

      assertThat(response.statusCode, is(200));
      assertThat(response.headers, allOf(
          hasEntry("Content-Length", Long.toString(data.length)),
          hasEntry("Content-Type", "text/plain")
      ));

      var bodyBytes =
          Optional.ofNullable(response.body)
                  .map(is -> Result.attempt(is::readAllBytes).orElseAssert());
      assertThat(bodyBytes.orElse(null), is(data));
    }
  }

  @Test
  void getArbitraryFileReturnsContents() {
    final var extensionsToMimes = Map.of(
      "txt", "text/plain",
      "jpg", "image/jpeg",
      "gif", "image/gif"
    );
    qt().forAll(paths(32),
                strings().allPossible().ofLengthBetween(1, 16).map(s -> s.getBytes()),
                pick(List.copyOf(extensionsToMimes.keySet())))
        .checkAssert((relSubdirPath, data, ext) -> {
          // create temp directory
          var baseDirFixture = new TempDirectoryFixture();
          // setup folder inside arbitrary temp dir
          var absSubdirPath = Paths.get(baseDirFixture.tempDirPath.toString(), relSubdirPath);
          Result.attempt(() ->  Files.createDirectories(absSubdirPath))
                .orElseAssert();
          // create file in arbitrary temp dir subfolder
          try (var tmpFileFixture = new TempFileFixture(absSubdirPath, "." + ext)) {
            // write arbitrary data
            tmpFileFixture.write(new ByteArrayInputStream(data));

            var responder = new FileHttpResponder(
                new LocalFilesystemController(baseDirFixture.tempDirPath));

            var relFilePath =
                baseDirFixture.tempDirPath.relativize(tmpFileFixture.tempFilePath).toString();

            var response = responder.getResponse(
                new RequestBuilder()
                    .withVersion(HttpVersion.ONE_ONE)
                    .withMethod(Method.GET)
                    .withUriString(relFilePath)
                    .build());

            assertThat(response.statusCode, is(200));
            assertThat(response.headers, allOf(
                hasEntry("Content-Length", Long.toString(data.length)),
                hasEntry("Content-Type", extensionsToMimes.get(ext))
            ));

            var bodyBytes =
                Optional.ofNullable(response.body)
                        .map(is -> Result.attempt(is::readAllBytes).orElseAssert());
            assertThat(bodyBytes.orElse(null), is(data));
          } finally {
            // close base temp directory
            baseDirFixture.close();
          }
        });
  }

  @Test
  void putUpdateFileReturns200() throws IOException {
    try (var tmpFileFixture = new TempFileFixture()) {
      var responder = new FileHttpResponder(
          new LocalFilesystemController(tmpFileFixture.tempFilePath.getParent()));
      var body = "foo".getBytes();
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.PUT)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .withBody(new ByteArrayInputStream(body))
              .withHeaders(Map.of(
                  "Content-Length", Integer.toString(body.length)
              ))
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.body, is(nullValue()));

      try (var newFileReader = new FileInputStream(tmpFileFixture.tempFilePath.toFile())) {
        assertThat(newFileReader.readAllBytes(), is(body));
      }
    }
  }
}
