package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.request.RequestParsingException;
import com.eighthlight.fabrial.http.request.RequestReader;
import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseBuilder;
import com.eighthlight.fabrial.http.response.ResponseWriter;
import com.eighthlight.fabrial.server.ConnectionHandler;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = LoggerFactory.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  private static final List<String> SUPPORTED_HTTP_VERSIONS = List.of(HttpVersion.ONE_ONE);

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
      request = new RequestReader(is).readRequest().get();
    } catch (NoSuchElementException e) {
      logger.trace("Skipping empty line.");
      return;
    } catch (RequestParsingException e) {
      logger.info("Failed to parse request, responding with 400", e);
      new ResponseWriter(os)
          .writeResponse(
              new ResponseBuilder()
                  .withVersion(HttpVersion.ONE_ONE)
                  .withStatusCode(400)
                  .build()
          );
      return;
    }

    logger.info("Handling request {}", StructuredArguments.kv("request", request));
    Response response = responseTo(request);
    logger.info("Writing response {}", StructuredArguments.kv("response", response));
    new ResponseWriter(os).writeResponse(response);
  }

  public Response responseTo(Request request) {
    if (!SUPPORTED_HTTP_VERSIONS.contains(request.version)) {
      return new ResponseBuilder()
          .withVersion(HttpVersion.ONE_ONE)
          .withStatusCode(501)
          .withReason("Requests must use one of the supported HTTP versions: "
                      + SUPPORTED_HTTP_VERSIONS)
          .build();
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
          logger.trace("No responder found");
          return new ResponseBuilder().withVersion(HttpVersion.ONE_ONE).withStatusCode(404).build();
        });
  }
}
