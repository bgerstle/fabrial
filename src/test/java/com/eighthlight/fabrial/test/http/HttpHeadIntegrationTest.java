package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import com.eighthlight.fabrial.test.fixtures.TcpServerFixture;
import com.eighthlight.fabrial.test.fixtures.TempFileFixture;
import com.eighthlight.fabrial.test.http.client.HttpClient;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static com.eighthlight.fabrial.http.HttpVersion.ONE_ONE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpHeadIntegrationTest {
  @Test
  void simpleHEADRequest() throws Throwable {
    try (TempFileFixture tempFileFixture = new TempFileFixture(Paths.get("/tmp"));
        TcpServerFixture serverFixture =
        new TcpServerFixture(new ServerConfig(ServerConfig.DEFAULT_PORT,
                                              ServerConfig.DEFAULT_READ_TIMEOUT,
                                              Paths.get("/tmp")));
        TcpClientFixture clientFixture =
            new TcpClientFixture(serverFixture.server.config.port)) {
      TcpServer server = serverFixture.server;
      server.start();
      clientFixture.client.connect();

      HttpClient client = new HttpClient(clientFixture.client);
      var response =
        client.send(
            new RequestBuilder()
                .withVersion(ONE_ONE)
                .withMethod(Method.HEAD)
                .withUriString(tempFileFixture.tempFilePath.getFileName().toString())
                .build()).get();
      assertThat(response.statusCode, equalTo(200));
    }
  }
}
