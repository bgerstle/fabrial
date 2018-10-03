package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.fixtures.TcpClientFixture;
import com.eighthlight.fabrial.test.fixtures.TcpServerFixture;
import com.eighthlight.fabrial.test.http.client.HttpClient;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccessLogIntegrationTest {
  @Test
  void emptyLogs() throws Exception {
    try (
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            ServerConfig.DEFAULT_DIRECTORY_PATH
        ));
        var clientFixture = new TcpClientFixture(ServerConfig.DEFAULT_PORT)) {
      serverFixture.server.start();
      clientFixture.client.connect();
      var client = new HttpClient(clientFixture.client);
      var response =
          client.send(new RequestBuilder()
                          .withVersion(HttpVersion.ONE_ONE)
                          .withMethod(Method.GET)
                          .withUriString("/logs")
                          .build())
                .get();
      assertThat(response.statusCode, equalTo(200));
    }
  }

  @Test
  void returnsPriorRequests() throws Exception {
    try (
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            ServerConfig.DEFAULT_DIRECTORY_PATH
        ))) {
      var methods = List.of(Method.GET, Method.PUT, Method.POST);
      methods
          .stream()
          .map(m -> new Request(HttpVersion.ONE_ONE, m, Result.attempt(() -> new URI("/")).orElseAssert()))
          .forEach(r -> {
            try (var client = new HttpClient(new TcpClient(new InetSocketAddress(serverFixture.server.config.port)))) {
              client.send(r);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });

      try (var client = new HttpClient(new TcpClient(new InetSocketAddress(serverFixture.server.config.port)))) {
        var response =
            client.send(new RequestBuilder()
                            .withVersion(HttpVersion.ONE_ONE)
                            .withUriString("/logs")
                            .build())
                  .get();
        assertThat(response.statusCode, equalTo(200));
        var bodyReader = new BufferedReader(new InputStreamReader(response.body));
        methods.forEach(m -> {
          var line = Result.attempt(bodyReader::readLine).orElseAssert();
          assertThat(line, containsString(m.name()));
        });
      }
    }
  }
}
