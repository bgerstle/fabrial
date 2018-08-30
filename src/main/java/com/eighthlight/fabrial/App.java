package com.eighthlight.fabrial;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
  static final Logger logger = Logger.getLogger(App.class.getName());

  public static void main(String[] args) {
    final TcpServer server = new TcpServer(new ServerConfig());

    try {
      server.start();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to start server", e);
      System.exit(1);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down");
      try {
        server.close();
      } catch (IOException e) {
        logger.log(Level.SEVERE, "Failed to close server", e);
        System.exit(1);
      }
    }));
  }
}
