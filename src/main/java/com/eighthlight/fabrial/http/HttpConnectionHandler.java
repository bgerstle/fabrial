package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.MessageReaderException;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.request.RequestReader;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.http.message.response.ResponseWriter;
import com.eighthlight.fabrial.server.ConnectionHandler;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = LoggerFactory.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  private static final List<String> SUPPORTED_HTTP_VERSIONS = List.of(HttpVersion.ONE_ONE);

  private final List<? extends HttpResponder> responders;

  private final Consumer<Request> requestDelegate;

  public HttpConnectionHandler(List<? extends HttpResponder> responders, Consumer<Request> requestDelegate) {
    this.requestDelegate = Optional.ofNullable(requestDelegate).orElse(r -> {});
    assert !responders.isEmpty();
    this.responders = responders;
  }

  @Override
  public void handleConnectionStreams(InputStream is, OutputStream os) throws Throwable {
    var reader = new RequestReader(is);
    var writer = new ResponseWriter(os);

    while (true) {
      Request request;
      try {
        request = reader.readRequest().get();
      } catch (NoSuchElementException e) {
        logger.trace("Encountered empty line, done reading requests");
        break;
      } catch (MessageReaderException e) {
        if (e.getCause() instanceof IOException) {
          logger.info("Done reading requests due to socket error", e);
          break;
        }
        logger.info("Failed to parse request, responding with 400", e);
        new ResponseWriter(os)
            .writeResponse(
                new ResponseBuilder()
                    .withVersion(HttpVersion.ONE_ONE)
                    .withStatusCode(400)
                    .build());
        continue;
      }

      logger.info("Handling request {}", StructuredArguments.kv("request", request));
      requestDelegate.accept(request);
      Response response = responseTo(request);
      logger.info("Writing response {}", StructuredArguments.kv("response", response));
      writer.writeResponse(response);

      if (!request.headers.getOrDefault("Connection", "").equals("Keep-Alive")) {
        logger.trace("No keep-alive header present, done parsing requests");
        break;
      }
    }
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
