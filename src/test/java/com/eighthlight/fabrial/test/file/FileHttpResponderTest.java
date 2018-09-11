package com.eighthlight.fabrial.test.file;

import com.eighthlight.fabrial.http.*;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.api.Subject1;
import org.quicktheories.core.Gen;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.paths;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
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
                equalTo(new Response(HttpVersion.ONE_ONE, expectedStatus, null)));
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
                equalTo(new Response(HttpVersion.ONE_ONE, 501, null)));
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
                equalTo(new Response(HttpVersion.ONE_ONE, 404, null)));
          });
        });
  }

  @Test
  void responds200WithAllowToOptionsOnExistingFiles() {
    forAllListsOfExistingAndNonExistingFiles()
        .checkAssert((files) -> {
          Set<Path> existingFilePaths = files.get(0);
          FileHttpResponder responder = responderForListOfExistingFiles(existingFilePaths);
          existingFilePaths.forEach(p -> {
            Request req =
                new Request(HttpVersion.ONE_ONE,
                            Method.OPTIONS,
                            Result.attempt(() -> new URI(p.toString())).orElseAssert());
            assertThat(
                responder.getResponse(req),
                equalTo(new Response(HttpVersion.ONE_ONE,
                                     200,
                                     null,
                                     Map.of("Allow", "HEAD,OPTIONS"))));
          });
        });
  }
}
