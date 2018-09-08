package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.RequestBuilder;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import com.eighthlight.fabrial.test.http.RequestWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.eighthlight.fabrial.http.HttpVersion.ONE_ONE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpHeadIntegrationTest extends TcpServerIntegrationTest {
  Path tempFilePath;

  @Override
  public TcpServer createServer() {
    try {
      Path tempDir = Files.createTempDirectory("test");
      tempFilePath = Files.createTempFile(tempDir,"test",null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new TcpServer(new ServerConfig(8080, 50, tempFilePath.getParent()));
  }

  @AfterEach
  void deleteTempFile() {
    try {
      Files.deleteIfExists(tempFilePath.getParent());
    } catch (IOException e) {}
  }

  @BeforeEach
  void startAndConnect() throws IOException {
    server.start();
    client.connect();
  }

  @Test
  void simpleHEADRequest() throws Throwable {
    new RequestWriter(client.getOutputStream())
        .writeRequest(
            new RequestBuilder()
                   .withVersion(ONE_ONE)
                   .withMethod(Method.HEAD)
                   .withUriString(tempFilePath.getFileName().toString())
                   .build());
    String response =
        new BufferedReader(new InputStreamReader((client.getInputStream())))
            .readLine();
    assertThat(response, containsString("200"));
  }
}
