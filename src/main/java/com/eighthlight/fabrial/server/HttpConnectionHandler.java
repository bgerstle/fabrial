package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = Logger.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  public static final List<String> SUPPORTED_HTTP_VERSIONS = List.of(HttpVersion.ONE_ONE);

  // TEMP
  public HttpConnectionHandler() {
    this.responders = Set.of(new HttpResponder() {
      @Override
      public boolean matches(Request request) {
        return request.uri.getPath().equals("/test");
      }

      @Override
      public Response getResponse(Request request) {
        if (!request.method.equals(Method.HEAD)) {
          return new Response(HttpVersion.ONE_ONE, 501, null);
        }
        return new Response(HttpVersion.ONE_ONE, 200, null);
      }
    });
  }

  public <T extends HttpResponder> HttpConnectionHandler(Set<T> responders) {
    assert !responders.isEmpty();
    this.responders = responders;
  }

  private final Set<? extends HttpResponder> responders;

  @Override
  public void handle(InputStream is, OutputStream os) throws Throwable {
    // TODO: handle multiple requests on one connection
    Request request;
    try {
      request = new RequestReader(is).readRequest();
    } catch (RequestParsingException e) {
      new Response(HttpVersion.ONE_ONE, 400, null).writeTo(os);
      return;
    }

    logger.info("Parsed request: " + request);
    Response response = responseTo(request);
    logger.fine("Writing response " + response);
    response.writeTo(os);
  }

  public Response responseTo(Request request) {
    if (!SUPPORTED_HTTP_VERSIONS.contains(request.version)) {
      return new Response(HttpVersion.ONE_ONE,
                          501,
                          "Requests must use one of the supported HTTP versions: "
                          + SUPPORTED_HTTP_VERSIONS);
    }

    Optional<? extends HttpResponder> responder =
        responders.stream()
                  .filter(r -> r.matches(request))
                  .findFirst();

    return responder.map(r -> {
      logger.trace("Found responder {}", StructuredArguments.kv("responder", r));
      return r.getResponse(request);
    })
                    .orElseGet(() -> {
                      logger.trace("No responder found");
                      return new Response(HttpVersion.ONE_ONE, 404, null);
                    });
  }
}
