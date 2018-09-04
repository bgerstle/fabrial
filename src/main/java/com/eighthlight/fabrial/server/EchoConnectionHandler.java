package com.eighthlight.fabrial.server;

import java.io.InputStream;
import java.io.OutputStream;

public class EchoConnectionHandler implements ConnectionHandler {
  @Override
  public void handle(InputStream is, OutputStream os) throws Throwable {
    is.transferTo(os);
  }
}
