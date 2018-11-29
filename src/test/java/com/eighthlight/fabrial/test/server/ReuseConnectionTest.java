package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.fixtures.TcpServerFixture;
import com.eighthlight.fabrial.test.http.client.HttpClient;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.zip;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.*;

public class ReuseConnectionTest {
  private static final Logger logger = LoggerFactory.getLogger(ReuseConnectionTest.class);

  @Test
  void persistentConnectionTest() throws Exception {
    qt().withExamples(50)
        .forAll(lists().of(integers().between(1, 50)).ofSizeBetween(4, 10),
                booleans().all())
        .checkAssert((requestCounts, parallel) -> {
          try (TcpServerFixture serverFixture =
              new TcpServerFixture(new ServerConfig(ServerConfig.DEFAULT_PORT,
                                                    ServerConfig.DEFAULT_READ_TIMEOUT,
                                                    ServerConfig.DEFAULT_DIRECTORY_PATH))) {

            Result.attempt(serverFixture.server::start).orElseAssert();

            var clientSuppliers = requestCounts
                .stream()
                .map(count -> {
                  return (Supplier<HttpClient>) () -> {
                    return Result.attempt(() -> {
                      var client = new TcpClient(new InetSocketAddress(serverFixture.server.config.port));
                      Result.attempt(() -> client.connect(500, 3, 100))
                            .orElseAssert();
                      return new HttpClient(client);
                    }).orElseAssert();
                  };
                })
                .collect(Collectors.toList());

            var request = new RequestBuilder()
                .withVersion(HttpVersion.ONE_ONE)
                .withUriString("/")
                .withMethod(Method.OPTIONS)
                .build();

            var futureResponseResults = Executors.newSingleThreadExecutor().submit(() -> {
              return zip(clientSuppliers.stream(),
                         parallel ? requestCounts.stream().parallel() : requestCounts.stream(),
                         (clientSupplier, requestCount) -> {
                           var client = clientSupplier.get();
                           var results = IntStream
                               .iterate(requestCount, i -> i > 0, i -> i - 1)
                               .mapToObj(i -> {
                                 return Result.<Response, Throwable>attempt(() -> {
                                   return client.send(request).get();
                                 });
                               })
                               .collect(Collectors.toList());
                           try {
                             client.close();
                           } catch (Exception e) {
                             logger.warn("Failed to close test client", e);
                           }
                           return results;
                         })
                  .map(List::stream)
                  .reduce(Stream.of(), Stream::concat)
                  .collect(Collectors.toList());
            });

            var responseResults = Result.attempt(() -> {
              return futureResponseResults.get(5, TimeUnit.SECONDS);
            }).orElseAssert();

            responseResults.forEach(result -> {
              assertThat(result.getError().isPresent(), equalTo(false));
              assertThat(result.map(r -> r.statusCode).getValue().orElse(null),
                         equalTo(200));
            });
          }
        });
  }
}
