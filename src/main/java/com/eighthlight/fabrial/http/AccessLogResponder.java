package com.eighthlight.fabrial.http;

import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import com.eighthlight.fabrial.server.BasicAuthResponder;
import com.eighthlight.fabrial.server.Credential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AccessLogResponder implements HttpResponder {
  private static final Logger logger = LoggerFactory.getLogger(AccessLogResponder.class);

  private final AccessLogger accessLogger;
  private final Optional<Credential> adminCredential;

  private static final String allowedMethods = String.join(",",
                                                           Method.GET.name(),
                                                           Method.HEAD.name(),
                                                           Method.OPTIONS.name());

  private static String formatRequestLog(RequestLog log) {
    return String.join(" ", log.method.name(), log.uri.getPath(), log.version);
  }

  public AccessLogResponder(AccessLogger accessLogger, Optional<Credential> adminCredential) {
    if (!adminCredential.isPresent()) {
      logger.warn("Auth disabled for /logs endpoint");
    }
    this.accessLogger = accessLogger;
    this.adminCredential = adminCredential;
  }

  @Override
  public boolean matches(Request request) {
    return request.uri.getPath().equals("/logs");
  }

  @Override
  public Response getResponse(Request request) {
    switch (request.method) {
      case HEAD: {
        return new ResponseBuilder().withVersion(request.version)
                                    .withStatusCode(200)
                                    .build();
      }
      case OPTIONS: {
        return new ResponseBuilder().withVersion(request.version)
                                    .withStatusCode(200)
                                    .withHeader("Allow", allowedMethods)
                                    .build();
      }
      case GET: {
        return getLogs(request);
      }
      default: {
        return new ResponseBuilder()
            .withVersion(request.version)
            .withStatusCode(405)
            .build();
      }
    }
  }

  private Response getLogs(Request request) {
    if (!adminCredential.isPresent()) {
      return logsResponse(request);
    }
    return new BasicAuthResponder().withExpectedCredential(adminCredential.get())
                                   .withAuthorizedResponder(this::logsResponse)
                                   .check(request);

  }

  private Response logsResponse(Request request) {
    var body =
        accessLogger
            .loggedRequests()
            .stream()
            .map(AccessLogResponder::formatRequestLog)
            .collect(Collectors.joining("\n"))
            .getBytes();

    return new ResponseBuilder()
        .withVersion(request.version)
        .withStatusCode(200)
        .withHeaders(Map.of(
            "Content-Length", Integer.toString(body.length),
            "Content-Type", "text/plain"
        ))
        .withBody(new ByteArrayInputStream(body))
        .build();
  }
}
