package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpConnectionHandler;
import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestBuilder;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.test.http.client.ResponseReader;
import com.eighthlight.fabrial.test.http.request.RequestWriter;
import com.bgerstle.result.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.pick;
import static org.quicktheories.generators.SourceDSL.lists;

public class HttpConnectionHandlerTests {
  public static Gen<String> supportedHttpVersions() {
    return pick(HttpConnectionHandler.SUPPORTED_HTTP_VERSIONS);
  }

  public static Gen<MockResponder> mockResponders() {
    return methods().zip(requestTargets(), statusCodes(), (method, uri, statusCode) ->
        new MockResponder(uri,
                          method.name(),
                          new ResponseBuilder().withVersion(HttpVersion.ONE_ONE)
                                               .withStatusCode(statusCode).build())
    );
  }

  public static Gen<List<MockResponder>> mockResponderLists() {
    return lists().of(mockResponders())
                  .ofSizeBetween(1, 10)
                  .map(rs -> rs.stream().distinct().collect(Collectors.toList()));
  }

  // Return response or throw an assertion if an error was encountered while handling the request.
  private Response handle(Request request, HttpConnectionHandler handler) {
    return handle(List.of(request), handler).get(0);
  }

  private List<Response> handle(List<Request> requests, HttpConnectionHandler handler) {
    var requestWriteStream = new ByteArrayOutputStream();
    var requestWriter = new RequestWriter(requestWriteStream);

    requests.forEach(r -> Result.attempt(() -> requestWriter.writeRequest(r)).orElseAssert());

    var requestInputStream = new ByteArrayInputStream(requestWriteStream.toByteArray());

    var responseOutputStream = new ByteArrayOutputStream();

    Result.attempt(() -> handler.handleConnectionStreams(requestInputStream, responseOutputStream))
          .orElseAssert();

    var serializedResponses = new ByteArrayInputStream(responseOutputStream.toByteArray());
    var responseReader = new ResponseReader(serializedResponses);

    // read response for each request
    return requests.stream()
                   .map(_unused -> Result.attempt(() -> responseReader.read().get()).orElseAssert())
                   .collect(Collectors.toList());
  }

  @Test
  void passesRequestToHandler() {
    qt().forAll(requests(methods(), requestTargets(), supportedHttpVersions()))
        .checkAssert(request -> {
          Result.attempt(() -> {
            var expectedResponse = new ResponseBuilder().withVersion(request.version)
                                                        .withStatusCode(200)
                                                        .build();

            var mockResponder = new MockResponder(request.uri, request.method, expectedResponse);

            var delegatedRequest = new CompletableFuture<Request>();

            HttpConnectionHandler handler =
                new HttpConnectionHandler(List.of(mockResponder), delegatedRequest::complete);

            var actualResponse = handle(request, handler);

            assertThat(actualResponse.statusCode, equalTo(expectedResponse.statusCode));

            assertThat(delegatedRequest.get(10, TimeUnit.MILLISECONDS), equalTo(request));
          }).orElseAssert();
        });
  }

  @Test
  void respondsWithMatchingResponder() {
    qt().forAll(mockResponderLists())
        .checkAssert(rs -> {
          HttpConnectionHandler handler = new HttpConnectionHandler(rs, null);
          rs.forEach(r -> {
            assertThat(
                handler.responseTo(new Request(HttpVersion.ONE_ONE,
                                               r.targetMethod,
                                               r.targetURI)),
                equalTo(r.response));
          });
        });
  }

  @Test
  void responds404WhenNoResponderFound() {
    qt().forAll(mockResponderLists(), http11Requests())
        .assuming((responders, req) ->
                      responders.stream().noneMatch(r -> r.matches(req))
        )
        .checkAssert((rs, req) -> {
          HttpConnectionHandler handler = new HttpConnectionHandler(rs, null);
          assertThat(handler.responseTo(req),
                     equalTo(new ResponseBuilder().withVersion(HttpVersion.ONE_ONE).withStatusCode(404).build()));
        });
  }

  @Test
  void throwsWhenRespondersEmpty() {
    assertThrows(AssertionError.class, () -> new HttpConnectionHandler(List.of(), null));
  }

  @Test
  void respondsToFirstMatchingResponder() {
    qt().forAll(mockResponders())
        .checkAssert(mockResponder -> {
          var badResponder = new HttpResponder() {
            @Override
            public boolean matches(Request request) {
              return mockResponder.matches(request);
            }

            @Override
            public Response getResponse(Request request) {
              throw new AssertionError("Should not be called");
            }
          };

          var handler = new HttpConnectionHandler(List.of(
              mockResponder,
              badResponder
          ), null);

          assertThat(handler.responseTo(new RequestBuilder()
                                            .withVersion(HttpVersion.ONE_ONE)
                                            .withMethodValue(mockResponder.targetMethod)
                                            .withUri(mockResponder.targetURI)
                                            .build()),
                     equalTo(mockResponder.response));
        });
  }

  @Test
  void addsConnectionCloseHeaderToHttpOneZeroRequestsWithoutKeepalive() {
    qt().forAll(mockResponders())
        .checkAssert(responder -> {
          HttpConnectionHandler handler =
              new HttpConnectionHandler(Arrays.asList(responder), null);

          var request = new Request(HttpVersion.ONE_ZERO,
                                    responder.targetMethod,
                                    responder.targetURI);

          var response = handle(request, handler);

          var expectedResponse =
              new ResponseBuilder(responder.getResponse(request))
                  .withHeader("Connection", "close")
                  .build();

          assertThat(response, equalTo(expectedResponse));
        });
  }

  @Test
  void addsKeepAliveHeaderToOneZeroRequestsWithKeepAlive() {
    qt().forAll(mockResponders(), pick(Arrays.asList("keep-alive", "Keep-Alive")))
        .checkAssert((responder, keepalive) -> {
          HttpConnectionHandler handler =
              new HttpConnectionHandler(Arrays.asList(responder), null);

          var request = new Request(HttpVersion.ONE_ZERO,
                                    responder.targetMethod,
                                    responder.targetURI,
                                    // checks for keep alive w/o case sensitivity
                                    Map.of("Connection", keepalive),
                                    null);

          var response = handle(request, handler);

          var expectedResponse =
              new ResponseBuilder(responder.getResponse(request))
                  .withHeader("Connection", "keep-alive")
                  .build();

          assertThat(response, equalTo(expectedResponse));
        });
  }

  @Test
  void assumesHttpOneOneIsKeepAlive() {
    qt().forAll(mockResponders())
        .checkAssert((responder) -> {
          HttpConnectionHandler handler =
              new HttpConnectionHandler(Arrays.asList(responder), null);

          var request = new Request(HttpVersion.ONE_ONE,
                                    responder.targetMethod,
                                    responder.targetURI);

          var response = handle(request, handler);

          var expectedResponse =
              new ResponseBuilder(responder.getResponse(request))
                  .withHeader("Connection", "keep-alive")
                  .build();

          assertThat(response, equalTo(expectedResponse));
        });
  }

  @Test
  void addsCloseResponseHeaderToRequestsWithClose() {
    qt().forAll(mockResponders(), supportedHttpVersions(), pick(List.of("close", "Close")))
        .checkAssert((responder, httpVersion, closeHeaderValue) -> {
          HttpConnectionHandler handler =
              new HttpConnectionHandler(Arrays.asList(responder), null);

          var request = new Request(httpVersion,
                                    responder.targetMethod,
                                    responder.targetURI,
                                    Map.of("Connection", closeHeaderValue),
                                    null);

          var response = handle(request, handler);

          var expectedResponse =
              new ResponseBuilder(responder.getResponse(request))
                  .withHeader("Connection", "close")
                  .build();

          assertThat(response, equalTo(expectedResponse));
        });
  }

  @Test
  void repsonds501ToUnsupportedHttpVersions() {
    qt().forAll(mockResponders(),
                pick(Arrays.asList(HttpVersion.ZERO_NINE, HttpVersion.TWO_ZERO)))
        .checkAssert((responder, httpVersion) -> {
          HttpConnectionHandler handler =
              new HttpConnectionHandler(Arrays.asList(responder), null);

          var request = new Request(httpVersion,
                                    responder.targetMethod,
                                    responder.targetURI,
                                    Map.of("Connection", "close"),
                                    null);

          var response = handle(request, handler);

          assertThat(response.statusCode, equalTo(501));
        });
  }

  @Test
  void handlesMultipleRequests() {
    qt().forAll(mockResponderLists())
        .checkAssert((responders) -> {
          HttpConnectionHandler handler =
              new HttpConnectionHandler(responders, null);

          var requests =
              responders
                  .stream()
                  .map(r -> new Request(HttpVersion.ONE_ONE, r.targetMethod, r.targetURI))
                  .collect(Collectors.toList());

          var responses = handle(requests, handler);

          var responseStatusCodes =
              responses.stream().map(r -> r.statusCode).collect(Collectors.toList());

          var expectedStatusCodes =
              responders.stream().map(r -> r.response.statusCode).collect(Collectors.toList());

          assertThat(responseStatusCodes, equalTo(expectedStatusCodes));
        });
  }
}
