package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpConnectionHandler;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.test.http.client.ResponseReader;
import com.eighthlight.fabrial.test.http.request.RequestWriter;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.constant;
import static org.quicktheories.generators.SourceDSL.lists;

public class HttpConnectionHandlerTests {

  public static Gen<MockResponder> mockResponders() {
    return methods().zip(requestTargets(), statusCodes(), (method, uri, statusCode) ->
        new MockResponder(uri,
                          method,
                          new ResponseBuilder().withVersion(HttpVersion.ONE_ONE)
                                               .withStatusCode(statusCode).build())
    );
  }

  public static Gen<List<MockResponder>> mockResponderLists() {
    return lists().of(mockResponders())
                  .ofSizeBetween(1, 10);
  }

  @Test
  void passesRequestToHandler() {
    qt().forAll(requests(methods(), requestTargets(), constant(HttpVersion.ONE_ONE)))
        .checkAssert(r -> {
          Result.attempt(() -> {
            var baos = new ByteArrayOutputStream();
            new RequestWriter(baos).writeRequest(r);
            var serializdRequestStream = new ByteArrayInputStream(baos.toByteArray());


            var expectedResponse = new ResponseBuilder().withVersion(HttpVersion.ONE_ONE)
                                                        .withStatusCode(200)
                                                        .build();

            var mockResponder = new MockResponder(r.uri, r.method, expectedResponse);

            var delegatedRequest = new CompletableFuture<Request>();

            HttpConnectionHandler handler =
                new HttpConnectionHandler(Set.of(mockResponder), delegatedRequest::complete);

            var responseBaos = new ByteArrayOutputStream();
            handler.handle(serializdRequestStream, responseBaos);

            var actualResponse =
                new ResponseReader(new ByteArrayInputStream(responseBaos.toByteArray())).read().get();
            assertThat(actualResponse, equalTo(expectedResponse));

            assertThat(delegatedRequest.get(10, TimeUnit.MILLISECONDS), equalTo(r));
          }).orElseAssert();
        });
  }

  @Test
  void respondsWithMatchingResponder() {
    qt().forAll(mockResponderLists().map(Set::copyOf))
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
    qt().forAll(mockResponderLists().map(Set::copyOf), http11Requests())
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
    assertThrows(AssertionError.class, () -> new HttpConnectionHandler(Set.of(), null));
  }
}
