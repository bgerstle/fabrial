package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.RequestBuilder;
import com.eighthlight.fabrial.test.file.TempDirectoryFixture;
import com.eighthlight.fabrial.test.file.TempFileFixtures;
import com.eighthlight.fabrial.test.http.AppProcessFixture;
import com.eighthlight.fabrial.test.http.RequestWriter;
import com.eighthlight.fabrial.test.http.TcpClientFixture;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag("acceptance")
public class AppAcceptanceTest {
  @Test
  void clientConnectsToAppServer() throws IOException {
    int testPort = 8081;
    try (AppProcessFixture appFixture = new AppProcessFixture(testPort , null);
        TcpClientFixture clientFixture = new TcpClientFixture(testPort )) {
      clientFixture.client.connect(1000, 3, 1000);
    }
  }

  @Test
  void sendHEADRequest() throws IOException {
    int testPort = 8082;
    int testDirDepth = 3;
    try (TempDirectoryFixture tempDirectoryFixture = TempFileFixtures.populatedTempDir(5, testDirDepth);
        AppProcessFixture appFixture = new AppProcessFixture(testPort, tempDirectoryFixture.tempDirPath.toString())) {
      Files
          .find(tempDirectoryFixture.tempDirPath,
                testDirDepth + 1,
                (p, attrs) -> true)
          .forEach((path) -> {
            assertThat(responseToHeadForFileInDir(tempDirectoryFixture.tempDirPath, path, testPort),
                       is("HTTP/1.1 200 "));
          });
    }
  }

  private static String responseToHeadForFileInDir(Path dir, Path path, int port) {
    {
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
                                .withMethod(Method.HEAD)
                                .build());
          return new BufferedReader(new InputStreamReader((is)))
              .lines()
              .filter((s) -> s != null && !s.isEmpty()).findFirst()
              .get();
        }
      }).orElseAssert();
    }
  }
}
