package com.eighthlight.fabrial;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
  static final Logger logger = Logger.getLogger(App.class.getName());

  public static ServerConfig parseConfig(String[] args) {
    // TODO get project name
    ArgumentParser parser = ArgumentParsers.newFor("fabrial").build()
        .description("Minimal HTTP file server.");

    parser.addArgument("-p").type(Integer.class).dest("port");
    try {
      Namespace ns = parser.parseArgs(args);
      int port =
          Optional.ofNullable(ns.getInt("port")).orElse(ServerConfig.DEFAULT_PORT);
      if (port <= 0 || port > 65535) {
        throw new ArgumentParserException("port must be between 1-65535", parser);
      }
      return new ServerConfig(port);
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      System.exit(1);
      return null;
    }
  }

  public static void main(String[] args) {
    final TcpServer server = new TcpServer(parseConfig(args));

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
