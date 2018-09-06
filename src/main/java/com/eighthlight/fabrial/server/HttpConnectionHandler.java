package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.RequestParsingException;
import com.eighthlight.fabrial.http.Response;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = Logger.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  public static final List<String> SUPPORTED_HTTP_VERSIONS = List.of(HttpVersion.ONE_ONE);

  @Override
  public void handle(InputStream is, OutputStream os) throws Throwable {
    // TODO: handle multiple requests on one connection
    Request request;
    try {
      request = Request.builder().buildWithStream(is);
    } catch (RequestParsingException e) {
      new Response(HttpVersion.ONE_ONE, 400, null).writeTo(os);
      return;
    }
    logger.info("Parsed request: " + request);
    Response response = responseTo(request);
    logger.info("Writing response: " + response);
    response.writeTo(os);
    os.flush();
  }

  public Response responseTo(Request request) {
    if (!SUPPORTED_HTTP_VERSIONS.contains(request.version)) {
      return new Response(HttpVersion.ONE_ONE,
                          501,
                          "Requests must use one of the supported HTTP versions: " + SUPPORTED_HTTP_VERSIONS);
    }
    switch (request.method) {
      case HEAD: {
        return responseToHEAD(request);
      }
      default: {
        return new Response(HttpVersion.ONE_ONE, 501, request.method + "not implemented yet");
      }
    }
  }


  public Response responseToHEAD(Request request) {
    if (request.uri.getPath().equals("/test")) {
      return new Response(HttpVersion.ONE_ONE, 200, null);
    } else {
      return new Response(HttpVersion.ONE_ONE, 404, null);
    }
  }
}
