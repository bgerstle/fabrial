package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixture;
import com.eighthlight.fabrial.test.file.TempFileFixtures;
import com.eighthlight.fabrial.test.http.AppProcessFixture;
import com.eighthlight.fabrial.test.http.RequestWriter;
import com.eighthlight.fabrial.test.http.TcpClientFixture;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;

@Tag("acceptance")
public class AppAcceptanceTest {
  private static final Logger logger = LoggerFactory.getLogger(AppAcceptanceTest.class);

  private static List<String> responseToRequestForFileInDir(Method method,
                                                            Path dir,
                                                            Path path,
                                                            int port) {
    String relPathStr =
        Paths.get("/",
                  dir.relativize(path)
                     .toString())
             .toString();
    return Result.attempt(() -> {
      // TEMP: need to recreate client for each file until multiple requests per conn is supported
      try (TcpClientFixture clientFixture = new TcpClientFixture(port)) {
        clientFixture.client.connect(1000, 3, 1000);
        OutputStream os = clientFixture.client.getOutputStream();
        InputStream is = clientFixture.client.getInputStream();
        new RequestWriter(os)
            .writeRequest(new RequestBuilder()
                              .withUriString(relPathStr)
                              .withVersion(HttpVersion.ONE_ONE)
                              .withMethod(method)
                              .build());
        return Arrays.asList(new BufferedReader(new InputStreamReader((is))).lines().toArray(String[]::new));
      }
    }).orElseAssert();
  }

  @Test
  void clientConnectsToAppServer() throws IOException {
    int testPort = 8081;
    try (AppProcessFixture appFixture = new AppProcessFixture(testPort , null);
        TcpClientFixture clientFixture = new TcpClientFixture(testPort )) {
      clientFixture.client.connect(1000, 3, 1000);
    }
  }

  @Test
  void headRequestForExistingFiles() throws IOException {
    int testPort = 8082;
    int testDirDepth = 3;
    try (TempDirectoryFixture tempDirectoryFixture = TempFileFixtures.populatedTempDir(5, testDirDepth);
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tempDirectoryFixture.tempDirPath.toString())) {
      Files
          .find(tempDirectoryFixture.tempDirPath,
                testDirDepth + 1,
                (p, attrs) -> true)
          .forEach((path) -> {
            assertThat(responseToRequestForFileInDir(Method.HEAD,
                                                     tempDirectoryFixture.tempDirPath,
                                                     path,
                                                     testPort).get(0),
                       is("HTTP/1.1 200 "));

            assertThat(responseToRequestForFileInDir(Method.HEAD,
                                                     tempDirectoryFixture.tempDirPath,
                                                     Paths.get(path.toString(), "doesntexist"),
                                                     testPort).get(0),
                       is("HTTP/1.1 404 "));
          });
    }
  }

  @Test
  void optionsToPath() throws IOException {
    int testPort = 8082;
    int testDirDepth = 3;
    try (TempDirectoryFixture tempDirectoryFixture = TempFileFixtures.populatedTempDir(5, testDirDepth);
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tempDirectoryFixture.tempDirPath.toString())) {
      Files
          .find(tempDirectoryFixture.tempDirPath,
                testDirDepth + 1,
                (p, attrs) -> true)
          .forEach((path) -> {
            var response = responseToRequestForFileInDir(Method.OPTIONS,
                                                         tempDirectoryFixture.tempDirPath,
                                                         path,
                                                         testPort);
            assertThat(response.get(0),
                       startsWith("HTTP/1.1 200 "));
            var headers = response.subList(1, 3);
            assertThat(headers, hasItem(allOf(
                startsWith("Allow: "),
                containsString("GET"),
                containsString("DELETE"),
                containsString("PUT"),
                containsString("HEAD"),
                not(containsString("POST")))));
            assertThat(headers, hasItem(equalTo("Content-Length: 0")));
          });
    }
  }

  @Test
  void getDirContents() throws IOException {
    int testPort = 8082;
    try (var tempDirectoryFixture = new TempDirectoryFixture();
        var tmpFileFixture1 = new TempFileFixture(tempDirectoryFixture.tempDirPath);
        var tmpFileFixture2 = new TempFileFixture(tempDirectoryFixture.tempDirPath);
        var appFixture = new AppProcessFixture(testPort, tempDirectoryFixture.tempDirPath.toString())) {
      Files
          .find(tempDirectoryFixture.tempDirPath,
                1,
                (p, attrs) -> attrs.isDirectory())
          .forEach((path) -> {
            var response = responseToRequestForFileInDir(Method.GET,
                                                         tempDirectoryFixture.tempDirPath,
                                                         path,
                                                         testPort);
            assertThat(response.get(0),
                       startsWith("HTTP/1.1 200 "));
            var headers = response.subList(1,3);
            var expectedBody =
                List.of(tmpFileFixture1, tmpFileFixture2)
                    .stream()
                    .map(t -> t.tempFilePath)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .sorted()
                    .reduce((p1, p2) -> p1 + "," + p2)
                    .get();
            var expectedLength = expectedBody.getBytes(StandardCharsets.UTF_8).length;
            assertThat(headers,
                       containsInAnyOrder(
                           "Content-Length: " + expectedLength,
                           "Content-Type: text/plain; charset=utf-8"));
            var body = response.get(4);
            assertThat(body, equalTo(expectedBody));
          });
    }
  }

  @Test
  void getEmptyDirContents() throws IOException {
    int testPort = 8082;
    try (var tempDirectoryFixture = new TempDirectoryFixture();
        var appFixture = new AppProcessFixture(testPort, tempDirectoryFixture.tempDirPath.toString())) {
      Files
          .find(tempDirectoryFixture.tempDirPath,
                1,
                (p, attrs) -> attrs.isDirectory())
          .forEach((path) -> {
            var response = responseToRequestForFileInDir(Method.GET,
                                                         tempDirectoryFixture.tempDirPath,
                                                         path,
                                                         testPort);
            assertThat(response.get(0),
                       startsWith("HTTP/1.1 200 "));
            var headers = response.subList(1,3);
            assertThat(headers,
                       containsInAnyOrder(
                           "Content-Length: " + 0,
                           "Content-Type: text/plain; charset=utf-8"));
            assertThat(response, hasSize(4));
          });
    }
  }
}
