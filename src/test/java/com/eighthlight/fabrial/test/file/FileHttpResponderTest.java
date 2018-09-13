package com.eighthlight.fabrial.test.file;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.http.file.FileResponderDataSourceImpl;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.http.response.ResponseBuilder;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.api.Subject1;
import org.quicktheories.core.Gen;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.paths;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.lists;

public class FileHttpResponderTest {
  static final Gen<Path> filePaths() {
    return paths(32).map(s -> {
      return Paths.get(s);
    });
  }

  static final Subject1<List<HashSet<Path>>> forAllListsOfExistingAndNonExistingFiles() {
    return qt().forAll(lists().of(filePaths()).ofSizeBetween(1, 5),
                       lists().of(filePaths()).ofSizeBetween(1, 5))
               .as((paths1, paths2) -> {
                 HashSet<Path> ps1 = new HashSet(paths1);
                 ps1.removeAll(paths2);
                 HashSet<Path> ps2 = new HashSet(paths2);
                 ps2.removeAll(paths1);
                 return List.of(ps1, ps2);
               });
  }

  static FileHttpResponder responderForListOfExistingFiles(Set<Path> files) {
    return new FileHttpResponder(new FileHttpResponder.DataSource() {
      @Override
      public boolean fileExistsAtPath(Path path) {
        return files.contains(path);
      }

      @Override
      public boolean isDirectory(Path path) {
        return false;
      }

      @Override
      public List<Path> getDirectoryContents(Path path) {
        return List.of();
      }

      @Override
      public long getFileSize(Path path) {
        throw new IllegalCallerException();
      }

      @Override
      public InputStream getFileContents(Path path) {
        throw new IllegalCallerException();
      }
    });
  }


  @Test
  void responds200ToHeadForExistingFiles() {
    forAllListsOfExistingAndNonExistingFiles()
        .checkAssert((files) -> {
          Set<Path> existingFilePaths = files.get(0);
          Set<Path> nonExistingFilePaths = files.get(1);
          FileHttpResponder responder = responderForListOfExistingFiles(existingFilePaths);
          ArrayList<Path> allFilePaths = new ArrayList<>();
          allFilePaths.addAll(existingFilePaths);
          allFilePaths.addAll(nonExistingFilePaths);
          allFilePaths.forEach(p -> {
            Request req = new Request(HttpVersion.ONE_ONE, Method.HEAD, Result.attempt(() -> new URI(p.toString())).orElseAssert());
            int expectedStatus = existingFilePaths.contains(p) ? 200 : 404;
            assertThat(
                responder.getResponse(req),
                equalTo(new ResponseBuilder()
                            .withVersion(HttpVersion.ONE_ONE)
                            .withStatusCode(expectedStatus)
                            .build()));
          });
        });
  }

  @Test
  void responds501ToUnsupportedMethodsOnExistingFiles() {
    forAllListsOfExistingAndNonExistingFiles()
        .checkAssert((files) -> {
          Set<Path> existingFilePaths = files.get(0);
          Set<Path> nonExistingFilePaths = files.get(1);
          FileHttpResponder responder = responderForListOfExistingFiles(existingFilePaths);
          existingFilePaths.forEach(p -> {
            Request req =
                new Request(HttpVersion.ONE_ONE,
                            Method.DELETE,
                            Result.attempt(() -> new URI(p.toString())).orElseAssert());
            assertThat(
                responder.getResponse(req),
                equalTo(new ResponseBuilder()
                            .withVersion(HttpVersion.ONE_ONE)
                            .withStatusCode(501)
                            .build()));
          });
        });
  }

  @Test
  void responds404ToUnsupportedMethodsOnNonExistingFiles() {
    forAllListsOfExistingAndNonExistingFiles()
        .checkAssert((files) -> {
          Set<Path> existingFilePaths = files.get(0);
          Set<Path> nonExistingFilePaths = files.get(1);
          FileHttpResponder responder = responderForListOfExistingFiles(existingFilePaths);
          nonExistingFilePaths.forEach(p -> {
            Request req =
                new Request(HttpVersion.ONE_ONE,
                            Method.DELETE,
                            Result.attempt(() -> new URI(p.toString())).orElseAssert());
            assertThat(
                responder.getResponse(req),
                equalTo(new ResponseBuilder()
                            .withVersion(HttpVersion.ONE_ONE)
                            .withStatusCode(404)
                            .build()));
          });
        });
  }

  @Test
  void responds200WithAllowToOptionsOnAnyPath() {
    forAllListsOfExistingAndNonExistingFiles()
        .checkAssert((files) -> {
          Set<Path> existingFilePaths = files.get(0);
          Set<Path> nonExistingFilePaths = files.get(1);
          FileHttpResponder responder = responderForListOfExistingFiles(existingFilePaths);
          ArrayList<Path> allFilePaths = new ArrayList<>();
          allFilePaths.addAll(existingFilePaths);
          allFilePaths.addAll(nonExistingFilePaths);
          allFilePaths.forEach(p -> {
            Request req =
                new Request(HttpVersion.ONE_ONE,
                            Method.OPTIONS,
                            Result.attempt(() -> new URI(p.toString())).orElseAssert());
            var resp = responder.getResponse(req);
            assertThat(resp.version, equalTo(HttpVersion.ONE_ONE));
            assertThat(resp.statusCode, equalTo(200));
            assertThat(resp.reason, nullValue());
            assertThat(Arrays.asList(resp.headers.get("Allow").split(", ")),
                       allOf(
                           containsInAnyOrder("GET", "HEAD", "OPTIONS", "PUT", "DELETE"),
                           not(contains("POST"))));
            assertThat(resp.headers.get("Content-Length"),
                       is("0"));
          });
        });
  }

  @Test
  void returnsAllFilesInDirWithMultipleFiles() throws IOException {
    try (var tmpDirFixture = new TempDirectoryFixture();
        var tmpFileFixture1 = new TempFileFixture(tmpDirFixture.tempDirPath);
        var tmpFileFixture2 = new TempFileFixture(tmpDirFixture.tempDirPath)) {
      var responder = new FileHttpResponder(
          new FileResponderDataSourceImpl(tmpDirFixture.tempDirPath));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString("/")
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, hasEntry("Content-Type", "text/plain; charset=utf-8"));
      var baos = new ByteArrayOutputStream();
      response.body.transferTo(baos);
      var bodyString = baos.toString();
      assertThat(bodyString,
                 allOf(
                     containsString(tmpFileFixture1.tempFilePath.getFileName().toString()),
                     containsString(tmpFileFixture2.tempFilePath.getFileName().toString())));
      assertThat(response.headers,
                 hasEntry("Content-Length",
                          Integer.toString(bodyString.getBytes(StandardCharsets.UTF_8).length)));
    }
  }

  @Test
  void getEmptyDirectoryResponseBodyIsEmpty() {
    try (var tmpDirFixture = new TempDirectoryFixture()) {
      var responder = new FileHttpResponder(
          new FileResponderDataSourceImpl(tmpDirFixture.tempDirPath));
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
          new FileResponderDataSourceImpl(tmpDirFixture.tempDirPath));
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
          new FileResponderDataSourceImpl(tmpFileFixture.tempFilePath.getParent()));
      var response = responder.getResponse(
          new RequestBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withMethod(Method.GET)
              .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
              .build());
      assertThat(response.statusCode, is(200));
      assertThat(response.headers, hasEntry("Content-Length", "0"));
    }
  }
}
