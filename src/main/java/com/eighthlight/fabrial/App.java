package com.eighthlight.fabrial;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
  static final Logger logger = Logger.getLogger(App.class.getName());

  static TcpServer server;

  public static void start() {
    server = new TcpServer(new ServerConfig());
    try {
      server.start();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to start server", e);
      System.exit(1);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(App::shutdown));
  }

  public static void run() {
    while (!server.isClosed()) {
      // let server run
    }
  }

  public static void shutdown() {
    try {
      server.close();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Failed to close server", e);
      System.exit(1);
    }
  }

  public static void main(String[] args) {
    start();
    run();
    logger.info("Goodbye!");
  }
}
