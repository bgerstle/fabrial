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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
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

@Tag("acceptance")
public class MultipleClientsAcceptanceTest {
  private static final Logger logger = LoggerFactory.getLogger(MultipleClientsAcceptanceTest.class);

  @Test
  void acceptsConnectionsAndRequestsFromUpToMaxConnections() throws Exception {
    qt().withExamples(50)
        .withShrinkCycles(0)
        .forAll(lists().of(integers().between(2, 5)).ofSizeBetween(2, 100))
        .checkAssert((requestCounts) -> {
          try (TcpServerFixture serverFixture =
              new TcpServerFixture(new ServerConfig(0,
                                                    ServerConfig.DEFAULT_READ_TIMEOUT,
                                                    ServerConfig.DEFAULT_DIRECTORY_PATH,
                                                    Optional.empty(),
                                                    requestCounts.size()))) {

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
            assertThat(serverFixture.server::getConnectionCount, eventuallyEval(equalTo(clients.size())));


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
                              logger.debug("Sending requests for client {}", client);
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

  @Test
  void acceptsStaggeredConnectionsAndRequestsFromUpToMaxConnections() throws Exception {
    var requestCountsGen = lists().of(integers().between(2, 5)).ofSizeBetween(2, 20);

    var delaysGen = requestCountsGen.mutate((requestCounts, rand) -> {
      return requestCounts.stream().map(requestCount -> {
        return lists().of(integers().between(100, 500)).ofSize(requestCount).generate(rand);
      });
    });

    qt().withExamples(5)
        .withShrinkCycles(0)
        .forAll(requestCountsGen, delaysGen)
        .checkAssert((requestCounts, delayPerRequest) -> {
          try (TcpServerFixture serverFixture =
              new TcpServerFixture(new ServerConfig(0,
                                                    ServerConfig.DEFAULT_READ_TIMEOUT,
                                                    ServerConfig.DEFAULT_DIRECTORY_PATH,
                                                    Optional.empty(),
                                                    requestCounts.size()))) {

            Result.attempt(serverFixture.server::start).orElseAssert();

            var request = new RequestBuilder()
                .withVersion(HttpVersion.ONE_ONE)
                .withUriString("/")
                .withMethod(Method.OPTIONS)
                .build();

            var requestService = Executors.newFixedThreadPool(requestCounts.size());

            var futureResponseResults =
                zip(delayPerRequest,
                    requestCounts.stream(),
                    (delays, requestCount) -> {
                      return requestService.submit(() -> {
                        // stagger the start of each batch of requests
                        Result.attempt(() -> Thread.sleep(delays.get(0) * 3)).orElseAssert();
                        var client = new TcpClient(new InetSocketAddress(serverFixture.server.getPort()));
                        Result.attempt(() -> client.connect()).orElseAssert();
                        var httpClient = new HttpClient(client);
                        var results = delays
                            .stream()
                            .map(delay -> {
                              // delay each request individually
                              Result.attempt(() -> Thread.sleep(delay)).orElseAssert();
                              return Result.<Response, Throwable>attempt(() -> {;
                                var response = httpClient.send(request).get();
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

            responseResults.forEach(result -> {
              assertThat(result.getError().isPresent(), equalTo(false));
              assertThat(result.map(r -> r.statusCode).getValue().orElse(null),
                         equalTo(200));
            });
          }
        });
  }

  @Test
  void acceptsStaggeredRequestsFromTransientConnections() throws Exception {
    var requestCountsGen = lists().of(integers().between(2, 5)).ofSizeBetween(2, 20);

    var delaysGen = requestCountsGen.mutate((requestCounts, rand) -> {
      return requestCounts.stream().map(requestCount -> {
        return lists().of(integers().between(100, 500)).ofSize(requestCount).generate(rand);
      });
    });

    qt().withExamples(5)
        .withShrinkCycles(0)
        .forAll(requestCountsGen, delaysGen)
        .checkAssert((requestCounts, delayPerRequest) -> {
          try (TcpServerFixture serverFixture =
              new TcpServerFixture(new ServerConfig(0,
                                                    ServerConfig.DEFAULT_READ_TIMEOUT,
                                                    ServerConfig.DEFAULT_DIRECTORY_PATH,
                                                    Optional.empty(),
                                                    requestCounts.size()))) {

            Result.attempt(serverFixture.server::start).orElseAssert();

            var request = new RequestBuilder()
                .withVersion(HttpVersion.ONE_ONE)
                .withUriString("/")
                .withMethod(Method.OPTIONS)
                .withHeaders(Map.of("Connection", "close"))
                .build();

            var requestService = Executors.newFixedThreadPool(requestCounts.size());

            var futureResponseResults =
                zip(delayPerRequest,
                    requestCounts.stream(),
                    (delays, requestCount) -> {
                      return requestService.submit(() -> {
                        return delays
                            .stream()
                            .map(delay -> {
                              Result.attempt(() -> Thread.sleep(delay)).orElseAssert();
                              var client = new TcpClient(new InetSocketAddress(serverFixture.server.getPort()));
                              Result.attempt(() -> client.connect()).orElseAssert();
                              var httpClient = new HttpClient(client);
                              var result = Result.<Response, Throwable>attempt(() -> {;
                                var response = httpClient.send(request).get();
                                return response;
                              });
                              Result.attempt(() -> client.close()).orElseAssert();
                              return result;
                            })
                            .collect(Collectors.toList());
                      });
                    })
                    .collect(Collectors.toList());

            var responseResults = futureResponseResults
                .stream()
                .parallel()
                .map(f -> Result.attempt(() -> f.get(20, TimeUnit.SECONDS)).orElseAssert())
                .collect(ArrayList<Result<Response, Throwable>>::new, ArrayList::addAll, ArrayList::addAll);

            responseResults.forEach(result -> {
              assertThat(result.getError().isPresent(), equalTo(false));
              assertThat(result.map(r -> r.statusCode).getValue().orElse(null),
                         equalTo(200));
            });
          }
        });
  }

  @Test
  void eventuallyRespondsToQueuedRequests() throws Exception {
    var maxConnGen = integers().between(2, Runtime.getRuntime().availableProcessors());
    var clientCountGen = maxConnGen.mutate((maxConnections, rand) -> {
      return maxConnections + integers().between(1, 50).generate(rand);
    });
    qt().forAll(maxConnGen, clientCountGen)
        .checkAssert((maxConnections, clientCount) -> {
          try (TcpServerFixture serverFixture =
              new TcpServerFixture(new ServerConfig(0,
                                                    ServerConfig.DEFAULT_READ_TIMEOUT,
                                                    ServerConfig.DEFAULT_DIRECTORY_PATH,
                                                    Optional.empty(),
                                                    maxConnections))) {

            Result.attempt(serverFixture.server::start).orElseAssert();

            // connect all clients at once
            var clients = IntStream
                .iterate(clientCount, i -> i > 0, i -> i - 1)
                .mapToObj(i -> {
                  var client = new TcpClient(new InetSocketAddress(serverFixture.server.getPort()));
                  Result.attempt(() -> client.connect()).orElseAssert();
                  return client;
                })
                .collect(Collectors.toList());

            // should assert that connectionCount == # of clients, but connection count isn't
            // incremented until the connection is pulled of the work queue (so connection count
            // is actually a representation of connections being handled, not connections accepted)

            // each client sends a request that should result in the connection being closed,
            // allowing other clients' requests to be handled
            var request = new RequestBuilder()
                .withVersion(HttpVersion.ONE_ONE)
                .withUriString("/")
                .withMethod(Method.OPTIONS)
                .withHeaders(Map.of("Connection", "close"))
                .build();

            var requestService = Executors.newFixedThreadPool(clientCount);

            var futureResponseResults = clients.stream().map(client -> {
                  return requestService.submit(() -> {
                    var httpClient = new HttpClient(client);
                    var result = Result.<Response, Throwable>attempt(() -> {
                      return httpClient.send(request).get();
                    });
                    return result;
                  });
                })
                .collect(Collectors.toList());

            var responseResults = futureResponseResults
                .stream()
                .parallel()
                .map(f -> Result.attempt(() -> f.get(20, TimeUnit.SECONDS)).orElseAssert())
                .collect(Collectors.toList());

            try {
              responseResults.forEach(result -> {
                assertThat(result.getError().isPresent(), equalTo(false));
                assertThat(result.map(r -> r.statusCode).getValue().orElse(null),
                           equalTo(200));
              });
            } catch (Exception e) {
              clients.forEach(c -> Result.attempt(() -> c.close()).orElseAssert());
            }
          }
        });
  }
}
