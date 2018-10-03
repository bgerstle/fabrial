package com.eighthlight.fabrial.test.server;

import com.eighthlight.fabrial.server.ConnectionHandler;

import java.io.InputStream;
import java.io.OutputStream;

class EchoConnectionHandler implements ConnectionHandler {
  @Override
  public void handle(InputStream is, OutputStream os) throws Throwable {
    is.transferTo(os);
  }
}
