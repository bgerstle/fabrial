package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.test.fixtures.AppProcessFixture;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import com.eighthlight.fabrial.test.fixtures.TempDirectoryFixture;
import com.eighthlight.fabrial.test.fixtures.TempFileFixture;
import com.eighthlight.fabrial.test.http.client.HttpClient;
import com.eighthlight.fabrial.test.http.request.RequestWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
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

  @Test
  void getExistingFile() throws IOException {
    int testPort = 8082;
    try (var tmpFileFixture = new TempFileFixture(Paths.get("/tmp"), ".txt");
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpFileFixture.tempFilePath.getParent().toString())) {
      var data = "bar".getBytes();
      tmpFileFixture.write(new ByteArrayInputStream(data));
      try (TcpClientFixture clientFixture = new TcpClientFixture(testPort);) {
        Result.attempt(() -> clientFixture.client.connect()).orElseAssert();
        var client = new HttpClient(clientFixture.client);

        var response =
            Result.attempt(() -> client.send(new RequestBuilder()
                                                 .withVersion(HttpVersion.ONE_ONE)
                                                 .withMethod(Method.GET)
                                                 .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
                                                 .build()))
                  .orElseAssert()
                  .get();

        assertThat(response.statusCode, is(200));
        assertThat(response.headers, allOf(
            hasEntry("Content-Length", Integer.toString(data.length)),
            hasEntry("Content-Type", "text/plain")
        ));
        assertThat(response.body.readAllBytes(), is(data));
      }
    }
  }

  @Test
  void getAbsentFile() throws IOException {
    int testPort = 8082;
    try (var tmpDirectoryFixture = new TempDirectoryFixture();
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tmpDirectoryFixture.tempDirPath.toString())) {
      try (TcpClientFixture clientFixture = new TcpClientFixture(testPort);) {
        clientFixture.client.connect();
        var client = new HttpClient(clientFixture.client);
        var response =
            Result.attempt(() -> client.send(new RequestBuilder()
                                                 .withVersion(HttpVersion.ONE_ONE)
                                                 .withMethod(Method.GET)
                                                 .withUriString("/foo")
                                                 .build()))
                  .orElseAssert()
                  .get();

        assertThat(response.statusCode, is(404));
        assertThat(response.body.readAllBytes(), is(new byte[0]));
      }
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
        try (TcpClientFixture clientFixture = new TcpClientFixture(testPort);) {
          Result.attempt(() -> clientFixture.client.connect()).orElseAssert();
          var client = new HttpClient(clientFixture.client);

          var response =
              Result.attempt(() -> client.send(new RequestBuilder()
                                                   .withVersion(HttpVersion.ONE_ONE)
                                                   .withMethod(Method.GET)
                                                   .withHeaders(Map.of(
                                                       "Range", "bytes=" + rangeStart + "-" + rangeEnd
                                                   ))
                                                   .withUriString(tmpFileFixture.tempFilePath.getFileName().toString())
                                                   .build()))
                    .orElseAssert()
                    .get();

          assertThat(response.statusCode, is(206));
          assertThat(response.headers, allOf(
              hasEntry("Content-Length", Integer.toString(s.length())),
              hasEntry("Content-Type", "text/plain"),
              hasEntry("Content-Range", "bytes " + rangeStart + "-" + rangeEnd  + "/" + data.length)
          ));
          assertThat(new String(response.body.readAllBytes()), is(s));
        }
      }
    }
  }
}
