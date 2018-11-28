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
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = LoggerFactory.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  public static final List<String> SUPPORTED_HTTP_VERSIONS = List.of(HttpVersion.ONE_ONE, HttpVersion.ONE_ZERO);

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
      Boolean shouldClose = false;
      Request request = null;
      Response response = null;

      try {
        request = reader.readRequest().get();
      } catch (NoSuchElementException e) {
        logger.trace("Encountered empty line, done reading requests");
        shouldClose = true;
        break;
      } catch (MessageReaderException e) {
        if (e.getCause() instanceof IOException) {
          logger.info("Done reading requests due to socket error", e);
          // Don't bother writing response
          break;
        } else {
          logger.info("Failed to parse request, responding with 400", e);
          response = new ResponseBuilder()
              .withVersion(HttpVersion.ONE_ONE)
              .withStatusCode(400)
              .build();
        }
      }

      if (response == null) {
        String connectionHeader = request.headers.getOrDefault("Connection", "");

        shouldClose |=
            (request.version.equals(HttpVersion.ONE_ZERO) && !connectionHeader.equalsIgnoreCase("keep-alive"))
            || connectionHeader.equalsIgnoreCase("close");

        logger.info("Handling request {}", StructuredArguments.kv("request", request));
        requestDelegate.accept(request);

        response = responseTo(request);
      }

      assert(response != null);

      response =
          new ResponseBuilder(response)
          .withHeader("Connection", shouldClose ? "close" : "keep-alive")
          .build();


      logger.info("Writing response {}", StructuredArguments.kv("response", response));
      writer.writeResponse(response);

      if (shouldClose) {
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
