package com.eighthlight.fabrial.test.file;

import com.eighthlight.fabrial.http.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  @Test
  void responds200ToHeadForExistingFiles() {
    qt().forAll(lists().of(filePaths()).ofSizeBetween(1, 5),
                lists().of(filePaths()).ofSizeBetween(1, 5))
        .as((paths1, paths2) -> {
          HashSet<Path> ps1 = new HashSet(paths1);
          ps1.removeAll(paths2);
          HashSet<Path> ps2 = new HashSet(paths2);
          ps2.removeAll(paths1);
          return List.of(ps1, ps2);
        })
        .checkAssert((files) -> {
          Set<Path> existingFilePaths = files.get(0);
          Set<Path> nonExistingFilePaths = files.get(1);
          FileHttpResponder responder = new FileHttpResponder(new FileHttpResponder.FileResponderDataSource() {
            @Override
            public boolean fileExistsAtPath(Path path) {
              return existingFilePaths.contains(path);
            }
          });
          ArrayList<Path> allFilePaths = new ArrayList<>();
          allFilePaths.addAll(existingFilePaths);
          allFilePaths.addAll(nonExistingFilePaths);
          allFilePaths.forEach(p -> {
            URI pathURI;
            try {
              pathURI = new URI(p.toString());
            } catch (URISyntaxException e) {
              throw new RuntimeException(e);
            }
            Request req = new Request(HttpVersion.ONE_ONE, Method.HEAD, pathURI);
            int expectedStatus = existingFilePaths.contains(p) ? 200 : 404;
            assertThat(
                responder.getResponse(req),
                equalTo(new Response(HttpVersion.ONE_ONE, expectedStatus, null)));
          });
        });
  }

  @Disabled
  void responds404ToHeadForNonExitingFiles() {

  }

  @Disabled
  void responds501ToUnsupportedMethodsOnExistingFiles() {

  }
}
