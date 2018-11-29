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
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.grantwest.eventually.EventuallyLambdaMatcher.eventuallyEval;
import static com.google.common.collect.Streams.zip;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;
import static org.quicktheories.generators.SourceDSL.lists;

public class MultipleClientsIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(MultipleClientsIntegrationTest.class);

  @Test
  void respondsToRequestFromMultipleClientsWithoutHanging() throws Exception {
    qt().withExamples(50)
        .withShrinkCycles(0)
        .forAll(lists().of(integers().between(2, 5)).ofSizeBetween(2, 20))
        .checkAssert((requestCounts) -> {
          try (TcpServerFixture serverFixture =
              new TcpServerFixture(new ServerConfig(0,
                                                    ServerConfig.DEFAULT_READ_TIMEOUT,
                                                    ServerConfig.DEFAULT_DIRECTORY_PATH))) {

            Result.attempt(serverFixture.server::start).orElseAssert();

            var clients = requestCounts
                .stream()
                .map(count -> {
                    var client = new TcpClient(new InetSocketAddress(serverFixture.server.getPort()));
                    Result.attempt(() -> client.connect())
                          .orElseAssert();
                    return new HttpClient(client);
                })
                .collect(Collectors.toList());


            // wait for all clients to connect
            assertThat(serverFixture.server::getConnectionCount,
                       eventuallyEval(equalTo(clients.size())));


            var request = new RequestBuilder()
                .withVersion(HttpVersion.ONE_ONE)
                .withUriString("/")
                .withMethod(Method.OPTIONS)
                .build();

            var requestService = Executors.newFixedThreadPool(clients.size());

            var futureResponseResults =
                zip(clients.stream(),
                    requestCounts.stream(),
                    (client, requestCount) -> {
                      return requestService.submit(() -> {
                        var results = IntStream
                            .iterate(requestCount, i -> i > 0, i -> i - 1)
                            .mapToObj(i -> {
                              return Result.<Response, Throwable>attempt(() -> {
                                var response = client.send(request).get();
                                return response;
                              });
                            })
                            .collect(Collectors.toList());
                        try {
                          client.close();
                        } catch (Exception e) {
                          logger.warn("Failed to close test client", e);
                        }
                        return results;
                      });
                    })
                    .collect(Collectors.toList());

            var responseResults = futureResponseResults
                .stream()
                .parallel()
                .map(f -> Result.attempt(() -> f.get(20, TimeUnit.SECONDS)).orElseAssert())
                .collect(ArrayList<Result<Response, Throwable>>::new, ArrayList::addAll, ArrayList::addAll);

            try {
              responseResults.forEach(result -> {
                assertThat(result.getError().isPresent(), equalTo(false));
                assertThat(result.map(r -> r.statusCode).getValue().orElse(null),
                           equalTo(200));
              });
            } finally {
              clients.forEach(c -> Result.attempt(c::close).orElseAssert());
            }
          }
        });
  }
}
