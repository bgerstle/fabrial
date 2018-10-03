package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.request.RequestBuilder;
import com.eighthlight.fabrial.test.fixtures.*;
import com.eighthlight.fabrial.test.http.request.RequestWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.AllOf.allOf;

@Tag("acceptance")
public class AppAcceptanceTest {
  private static final Logger logger = LoggerFactory.getLogger(AppAcceptanceTest.class);

  private static List<String> sendRequest(Request request, int port) throws IOException {
    try (TcpClientFixture clientFixture = new TcpClientFixture(port)) {
      clientFixture.client.connect(1000, 3, 1000);
      OutputStream os = clientFixture.client.getOutputStream();
      InputStream is = clientFixture.client.getInputStream();
      new RequestWriter(os).writeRequest(request);
      return Arrays.asList(new BufferedReader(new InputStreamReader((is))).lines().toArray(String[]::new));
    }
  }

  private static List<String> responseToRequestForFileInDir(Method method,
                                                            Path dir,
                                                            Path path,
                                                            int port) {
    String relPathStr =
        Paths.get("/",
                  dir.toAbsolutePath().relativize(path).toString())
             .toString();
    return Result.attempt(() -> {
      // TEMP: need to recreate client for each file until multiple requests per conn is supported
      return sendRequest(new RequestBuilder()
                             .withUriString(relPathStr)
                             .withVersion(HttpVersion.ONE_ONE)
                             .withMethod(method)
                             .build(),
                         port);
    }).orElseAssert();
  }

  @Disabled
  void clientConnectsToAppServer() throws IOException {
    int testPort = 8081;
    try (AppProcessFixture appFixture = new AppProcessFixture(testPort , null);
        TcpClientFixture clientFixture = new TcpClientFixture(testPort)) {
      clientFixture.client.connect(1000, 3, 1000);
    }
  }

  @Disabled
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
  void getExistingFile() {
    int testPort = 8082;
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt");
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpFileFixture.tempFilePath.getParent().toString())) {
      var data = "bar".getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(data));
      var responseLines = responseToRequestForFileInDir(Method.GET,
                                                        tmpFileFixture.tempFilePath.getParent(),
                                                        tmpFileFixture.tempFilePath,
                                                        testPort);
      assertThat(responseLines, hasSize(5));
      assertThat(responseLines.get(0),
                 is("HTTP/1.1 200 "));
      assertThat(responseLines.subList(1, 3),
                 containsInAnyOrder("Content-Length: " + data.length,
                                    "Content-Type: text/plain"));
      assertThat(responseLines.get(4).getBytes(), is(data));
    }
  }

  @Test
  void getAbsentFile() {
    int testPort = 8082;
    try (var tmpDirectoryFixture = new TempDirectoryFixture();
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpDirectoryFixture.tempDirPath.toString())) {
      var responseLines = responseToRequestForFileInDir(Method.GET,
                                                        tmpDirectoryFixture.tempDirPath,
                                                        Paths.get(tmpDirectoryFixture.tempDirPath.toString(), "/foo"),
                                                        testPort);
      assertThat(responseLines, hasSize(1));
      assertThat(responseLines.get(0),
                 is("HTTP/1.1 404 "));
    }
  }

  @Disabled
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

  @Disabled
  void getEmptyDirContents() throws IOException {
    int testPort = 8082;
    try (var baseDirFixture = new TempDirectoryFixture();
        var appFixture = new AppProcessFixture(testPort, baseDirFixture.tempDirPath.toString())) {
      var response = Result.attempt(() -> {
        return sendRequest(new RequestBuilder()
                               .withVersion(HttpVersion.ONE_ONE)
                               .withMethod(Method.GET)
                               .withUriString("/")
                               .build(),
                           testPort);
      }).orElseAssert();

      assertThat(response.get(0),
                 startsWith("HTTP/1.1 200 "));
      var headers = response.subList(1,3);
      assertThat(response.get(1), is("Content-Length: 0"));
    }
  }

  @Disabled
  void createThenUpdateFile() throws IOException {
    int testPort = 8082;
    try (var tmpDirectoryFixture = new TempDirectoryFixture();
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpDirectoryFixture.tempDirPath.toString())) {
      var body = "foo".getBytes();
      var responseLines =
          sendRequest(new RequestBuilder()
                          .withVersion(HttpVersion.ONE_ONE)
                          .withMethod(Method.PUT)
                          .withUriString("/foo")
                          .withBody(new ByteArrayInputStream(body))
                          .withHeaders(Map.of(
                              "Content-Length", Integer.toString(body.length)
                          ))
                          .build(),
                      testPort);
      assertThat(responseLines, hasSize(1));
      assertThat(responseLines.get(0),
                 is("HTTP/1.1 201 "));

      var newFilePath = Paths.get(
          tmpDirectoryFixture.tempDirPath.toString(),
          "foo"
      );
      try (var fileReader = new FileInputStream(newFilePath.toFile())) {
        assertThat(fileReader.readAllBytes(), is(body));
      }

      var body2 = "bar".getBytes();

      var responseLines2 =
          sendRequest(new RequestBuilder()
                          .withVersion(HttpVersion.ONE_ONE)
                          .withMethod(Method.PUT)
                          .withUriString("/foo")
                          .withBody(new ByteArrayInputStream(body2))
                          .withHeaders(Map.of(
                              "Content-Length", Integer.toString(body2.length)
                          ))
                          .build(),
                      testPort);
      assertThat(responseLines2, hasSize(1));
      assertThat(responseLines2.get(0),
                 is("HTTP/1.1 200 "));

      try (var fileReader = new FileInputStream(newFilePath.toFile())) {
        assertThat(fileReader.readAllBytes(), is(body2));
      }
    }
  }

  @Disabled
  void deleteFile() throws IOException {
    int testPort = 8082;
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt");
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpFileFixture.tempFilePath.getParent().toString())) {
      var responseLines =
          sendRequest(new RequestBuilder()
                          .withVersion(HttpVersion.ONE_ONE)
                          .withMethod(Method.DELETE)
                          .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
                          .build(),
                      testPort);
      assertThat(responseLines, hasSize(1));
      assertThat(responseLines.get(0),
                 is("HTTP/1.1 200 "));

      assertThat(tmpFileFixture.tempFilePath.toFile().exists(), is(false));
    }
  }

  @Test
  void getFileInParts() throws IOException {
    int testPort = 8082;
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt");
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpFileFixture.tempFilePath.getParent().toString())) {
      var strings = List.of("bar", "baz", "buz");
      var data = String.join("", strings).getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(data));

      for (int i = 0; i < strings.size(); i++) {
        var s = strings.get(i);
        var rangeStart = s.length() * i;
        var rangeEnd = s.length() + rangeStart - 1;
        var responseLines =
            sendRequest(new RequestBuilder()
                            .withVersion(HttpVersion.ONE_ONE)
                            .withMethod(Method.GET)
                            .withHeaders(Map.of(
                                "Range", "bytes=" + rangeStart + "-" + rangeEnd
                            ))
                            .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
                            .build(),
                        testPort);
        assertThat(responseLines, hasSize(6));
        assertThat(responseLines.get(0),
                   is("HTTP/1.1 206 "));
        assertThat(responseLines.subList(1, 4),
                   containsInAnyOrder(
                       "Content-Type: text/plain",
                       "Content-Length: " + s.length(),
                       "Content-Range: " + "bytes " + rangeStart + "-" + rangeEnd  + "/" + data.length));
        assertThat(responseLines.get(5), is(s));
      }
    }
  }
}
