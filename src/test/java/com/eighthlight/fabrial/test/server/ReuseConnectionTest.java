package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import com.eighthlight.fabrial.test.fixtures.TcpServerFixture;
import com.eighthlight.fabrial.test.http.client.HttpClient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReuseConnectionTest {
  @Test
  void respondsToMultipleRequestsOnOneConnection() throws Exception {
    try (TcpServerFixture serverFixture =
        new TcpServerFixture(new ServerConfig(ServerConfig.DEFAULT_PORT,
                                              ServerConfig.DEFAULT_READ_TIMEOUT,
                                              ServerConfig.DEFAULT_DIRECTORY_PATH));
        TcpClientFixture clientFixture =
            new TcpClientFixture(serverFixture.server.config.port)) {
      serverFixture.server.start();
      clientFixture.client.connect(500, 3, 100);
      var httpClient = new HttpClient(clientFixture.client);
      var request = new RequestBuilder()
          .withVersion(HttpVersion.ONE_ONE)
          .withUriString("/")
          .withMethod(Method.OPTIONS)
          .build();
      var resp1 = httpClient.send(request);
      var resp2 = httpClient.send(request);
      List.of(resp1, resp2).forEach(r -> {
        assertThat(r.isPresent(), equalTo(true));
        assertThat(r.get().statusCode, equalTo(200));
      });
    }
  }
}
