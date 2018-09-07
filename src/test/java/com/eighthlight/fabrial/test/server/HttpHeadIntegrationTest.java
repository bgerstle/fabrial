package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    Request.builder()
           .withVersion(ONE_ONE)
           .withMethod(Method.HEAD)
           .withUriString(Paths.get("/", tempFilePath.getFileName().toString()).toString())
                               .build()
                               .writeTo(client.getOutputStream());

    BufferedReader reader = new BufferedReader(new InputStreamReader((client.getInputStream())));
    String response = reader.readLine();
    assertThat(response, containsString("200"));
  }
}
