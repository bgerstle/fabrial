package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.Response;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Logger;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = Logger.getLogger(HttpConnectionHandler.class.getName());

  // ???: if this changes, probably need to also "match" the request version somehow?
  public static final List<String> supportedHttpVersions = List.of(HttpVersion.ONE_ONE);

  @Override
  public void handle(InputStream is, OutputStream os) throws Throwable {
    // TODO: handle multiple requests on one connection
    Request request = Request.readFrom(is);
    logger.info("Parsed request: " + request);
    Response response = responseTo(request);
    logger.info("Writing response: " + response);
    response.writeTo(os);
    os.flush();
  }

  public Response responseTo(Request request) {
    switch (request.method) {
      case HEAD: {
        return responseToHEAD(request);
      }
      default: {
        return new Response(HttpVersion.ONE_ONE, 501, null);
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
