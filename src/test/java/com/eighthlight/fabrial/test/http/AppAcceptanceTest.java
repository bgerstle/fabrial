package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.test.fixtures.AppProcessFixture;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import com.eighthlight.fabrial.test.fixtures.TempDirectoryFixture;
import com.eighthlight.fabrial.test.fixtures.TempFileFixture;
import com.eighthlight.fabrial.test.http.client.HttpClient;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.AllOf.allOf;

@Tag("acceptance")
public class AppAcceptanceTest {
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
