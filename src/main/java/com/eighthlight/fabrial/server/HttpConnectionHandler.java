package com.eighthlight.fabrial.server;

import com.eighthlight.fabrial.http.Request;
import com.eighthlight.fabrial.http.Response;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class HttpConnectionHandler implements ConnectionHandler {
  private static final Logger logger = Logger.getLogger(HttpConnectionHandler.class.getName());

  @Override
  public void handle(InputStream is, OutputStream os) throws Throwable {
    // TODO: handle multiple requests on one connection
    Request request = Request.readFrom(is);
    logger.fine("Parsed request :" + request);
    Response response = responseTo(request);
    logger.fine("Writing response: " + response);
    response.writeTo(os);
    os.flush();
  }

  public Response responseTo(Request request) {
    switch (request.method) {
      case HEAD: {
        return responseToHEAD(request);
      }
      default: {
        return Response.withStatus(501);
      }
    }
  }


  public Response responseToHEAD(Request request) {
    if (request.uri.getPath().equals("/test")) {
      return Response.withStatus(200);
    } else {
      return Response.withStatus(404);
    }
  }
}
