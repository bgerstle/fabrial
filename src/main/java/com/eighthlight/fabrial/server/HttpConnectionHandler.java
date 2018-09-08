package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.*;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = LoggerFactory.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  public static final List<String> SUPPORTED_HTTP_VERSIONS = List.of(HttpVersion.ONE_ONE);

  public HttpConnectionHandler() {
    this.responders = Set.of(
        new FileHttpResponder(
            new FileResponderDataSourceImpl(null)));
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
    try (MDC.MDCCloseable reqctxt = MDC.putCloseable("request", request.toString())) {
      logger.trace("Handling request");
      Response response = responseTo(request);
      logger.trace("Writing response {}", StructuredArguments.kv("response", response));
      response.writeTo(os);
    }
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

    return responder
        .map(r -> {
          logger.trace("Found responder {}", StructuredArguments.kv("responder", r));
          return r.getResponse(request);
        })
        .orElseGet(() -> {
          logger.debug("No responder found");
          return new Response(HttpVersion.ONE_ONE, 404, null);
        });
  }
}
