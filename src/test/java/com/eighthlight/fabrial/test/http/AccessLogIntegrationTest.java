package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.auth.BasicAuth;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.server.Credential;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class AccessLogIntegrationTest {
  @Test
  void unauthorized() throws Exception {
    var credential = new Credential("foo", "bar");
    try (
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            ServerConfig.DEFAULT_DIRECTORY_PATH,
            Optional.of(credential)));
        var clientFixture = new TcpClientFixture(ServerConfig.DEFAULT_PORT)) {
      serverFixture.server.start();
      clientFixture.client.connect();
      var client = new HttpClient(clientFixture.client);
      var response =
          client.send(new RequestBuilder()
                          .withVersion(HttpVersion.ONE_ONE)
                          .withMethod(Method.GET)
                          .withHeaders(BasicAuth.encode(new Credential("bar", "foo")))
                          .withUriString("/logs")
                          .build())
                .get();
      assertThat(response.statusCode, equalTo(401));
    }
  }

  @Test
  void emptyLogs() throws Exception {
    try (
        var serverFixture = new TcpServerFixture(new ServerConfig(
            ServerConfig.DEFAULT_PORT,
            ServerConfig.DEFAULT_READ_TIMEOUT,
            ServerConfig.DEFAULT_DIRECTORY_PATH));
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
            ServerConfig.DEFAULT_DIRECTORY_PATH))) {

      serverFixture.server.start();

      List<String> methods =
          Stream.of(Method.GET, Method.PUT, Method.POST)
                .map(Method::name)
                .collect(Collectors.toList());

      methods
          .stream()
          .map(m -> new Request(HttpVersion.ONE_ONE, m, Result.attempt(() -> new URI("/")).orElseAssert()))
          .forEach(r -> {
            try (var client = new TcpClient(new InetSocketAddress(serverFixture.server.config.port))) {
              client.connect();
              new HttpClient(client).send(r);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });

      try (var client = new TcpClient(new InetSocketAddress(serverFixture.server.config.port))) {
        client.connect();
        var response =
            new HttpClient(client)
                .send(new RequestBuilder()
                          .withVersion(HttpVersion.ONE_ONE)
                          .withMethod(Method.GET)
                          .withUriString("/logs")
                          .build())
                .get();
        assertThat(response.statusCode, equalTo(200));
        var bodyReader = new BufferedReader(new InputStreamReader(response.body));
        methods.forEach(m -> {
          var line = Result.attempt(bodyReader::readLine).orElseAssert();
          assertThat(line, containsString(m));
        });
      }
    }
  }
}
