package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.RequestBuilder;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.http.RequestWriter;
import com.eighthlight.fabrial.test.http.TcpClientFixture;
import com.eighthlight.fabrial.test.http.TempFileFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Tag("acceptance")
public class AppAcceptanceTest {
  static final Logger logger = LoggerFactory.getLogger(AppAcceptanceTest.class.getName());

  Process appProcess;

  @BeforeEach
  void setUp() throws IOException {
    // hard-coded to SNAPSHOT version. might need to fix this eventually...
    appProcess =
        new ProcessBuilder(
            "java",
            // disable logback error messages (no logstash here)
            "-Dlogback.statusListenerClass=ch.qos.logback.core.status.NopStatusListener",
            "-jar", "./build/libs/fabrial-all-1.0-SNAPSHOT.jar")
            .inheritIO()
            .start();
  }

  @AfterEach
  void tearDown() {
    appProcess.destroy();
  }

  @Test
  void clientConnectsToAppServer() throws IOException {
    try (TcpClientFixture clientFixture =
        new TcpClientFixture(ServerConfig.DEFAULT_PORT)) {
      clientFixture.client.connect(1000, 3, 1000);
    }
  }

  @Test
  void sendHEADRequest() throws IOException {
    try (TcpClientFixture clientFixture = new TcpClientFixture(ServerConfig.DEFAULT_PORT);
        // TEMP: serve from current directory
        // TODO: set -d to tmp dir
        TempFileFixture tempFileFixture = new TempFileFixture(Paths.get("."))) {
      clientFixture.client.connect(1000, 3, 1000);
      new RequestWriter(clientFixture.client.getOutputStream())
          .writeRequest(new RequestBuilder()
                            .withUriString(tempFileFixture.tempFilePath.getFileName().toString())
                            .withVersion(HttpVersion.ONE_ONE)
                            .withMethod(Method.HEAD)
                            .build());
      String response =
          new BufferedReader(new InputStreamReader((clientFixture.client.getInputStream())))
              .readLine();
      assertThat(response, is("HTTP/1.1 200 "));
    }
  }
}
