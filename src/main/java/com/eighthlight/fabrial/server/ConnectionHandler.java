package com.eighthlight.fabrial.server;

import java.io.InputStream;
import java.io.OutputStream;

public interface ConnectionHandler {
  void handleConnectionStreams(InputStream is, OutputStream os) throws Throwable;
}
