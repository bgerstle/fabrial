package com.eighthlight.fabrial;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
  static final Logger logger = Logger.getLogger(App.class.getName());

  public static Optional<ServerConfig> parseConfig(String[] args) {
    // TODO run project name
    ArgumentParser parser = ArgumentParsers.newFor("fabrial").build()
        .description("Minimal HTTP file server.");

    parser.addArgument("-p")
          .type(Integer.class)
          .setDefault(ServerConfig.DEFAULT_PORT)
          .dest("port")
          .help("A port number between 0-65535. Specifying 0 will cause the server to listen on a random port.");

    parser.addArgument("-d")
          .setDefault(ServerConfig.DEFAULT_DIRECTORY_PATH.toString())
          .dest("directory")
          .help("Directory that files should be served from. Defaults to current working directory.");

    try {
      Namespace ns = parser.parseArgs(args);

      int port = ns.getInt("port");
      if (port < 0 || port > 65535) {
        throw new ArgumentParserException("port must be between 0-65535", parser);
      }

      String pathString = ns.getString("directory");
      Path directoryPath;
      try {
        directoryPath = Paths.get(pathString).toAbsolutePath();
      } catch (InvalidPathException e) {
        throw new ArgumentParserException("\"" + pathString + "\" is not a valid path",
                                          e,
                                          parser);
      }

      return Optional.of(new ServerConfig(port,
                                          ServerConfig.DEFAULT_READ_TIMEOUT,
                                          directoryPath));
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      return Optional.empty();
    }
  }

  public static void main(String[] args) {
    LogConfig.apply();

    Optional<ServerConfig> config = parseConfig(args);
    if (!config.isPresent()) {
      System.exit(1);
    }
    final TcpServer server = new TcpServer(config.get());

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
