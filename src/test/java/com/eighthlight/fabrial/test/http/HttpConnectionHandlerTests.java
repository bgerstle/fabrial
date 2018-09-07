package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.*;
import com.eighthlight.fabrial.server.HttpConnectionHandler;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.eighthlight.fabrial.test.http.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.lists;

public class HttpConnectionHandlerTests {
  public static class MockResponder implements HttpResponder {
    public final URI targetURI;
    public final Method targetMethod;
    public final Response response;

    public MockResponder(URI targetURI, Method targetMethod, Response response) {
      this.targetURI = targetURI;
      this.targetMethod = targetMethod;
      this.response = response;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      MockResponder that = (MockResponder) o;
      // intentionally excluding response
      return Objects.equals(targetURI, that.targetURI) &&
             Objects.equals(targetMethod, that.targetMethod);
    }

    @Override
    public String toString() {
      return "MockResponder{" +
             "targetURI=" + targetURI +
             ", targetMethod=" + targetMethod +
             ", response=" + response +
             '}';
    }

    @Override
    public int hashCode() {
      // intentionally excluding response
      return Objects.hash(targetURI, targetMethod);
    }

    @Override
    public boolean matches(Request request) {
      return request.uri.equals(targetURI) && request.method.equals(targetMethod);
    }

    @Override
    public Response getResponse(Request request) {
      return response;
    }
  }

  public static Gen<MockResponder> mockResponders() {
    return methods().zip(requestTargets(), statusCodes(), (method, uri, statusCode) ->
        new MockResponder(uri,
                          method,
                          new Response(HttpVersion.ONE_ONE, statusCode, null))
    );
  }

  public static Gen<List<MockResponder>> mockResponderLists() {
    return lists().of(mockResponders())
                  .ofSizeBetween(1, 10);
  }

  @Test
  void respondsWithMatchingResponder() {
    qt().forAll(mockResponderLists().map(Set::copyOf))
        .checkAssert(rs -> {
          HttpConnectionHandler handler = new HttpConnectionHandler(rs);
          rs.forEach(r ->
           assertThat(
               handler.responseTo(new Request(HttpVersion.ONE_ONE, r.targetMethod, r.targetURI)),
               equalTo(r.response)
          ));
        });
  }

  @Test
  void responds404WhenNoResponderFound() {
    qt().forAll(mockResponderLists().map(Set::copyOf), http11Requests())
        .assuming((responders, req) ->
          responders.stream().noneMatch(r -> r.matches(req))
        )
        .checkAssert((rs, req) -> {
          HttpConnectionHandler handler = new HttpConnectionHandler(rs);
          assertThat(handler.responseTo(req),
                     equalTo(new Response(HttpVersion.ONE_ONE, 404, null)));
        });
  }

  @Test
  void throwsWhenRespondersEmpty() {
    assertThrows(AssertionError.class, () -> new HttpConnectionHandler(Set.of()));
  }
}
