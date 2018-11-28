package com.eighthlight.fabrial;

import com.eighthlight.fabrial.server.ServerConfig;
import com.eighthlight.fabrial.server.TcpServer;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.eighthlight.fabrial.server.AdminCredentials.fromEnvironment;

public class App {
  static final Logger logger = LoggerFactory.getLogger(App.class);

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

    parser.addArgument("--read-timeout")
          .type(Integer.class)
          .setDefault(ServerConfig.DEFAULT_READ_TIMEOUT)
          .dest("readTimeout")
          .help("Number of milliseconds the server will wait to read data from a socket.");

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

      int readTimeout = ns.getInt("readTimeout");
      if (readTimeout < 0) {
        throw new ArgumentParserException("Read timeout must be greater than or equal to 0.", parser);
      }

      return Optional.of(new ServerConfig(port,
                                          readTimeout,
                                          directoryPath,
                                          fromEnvironment(System.getenv())));
    } catch (ArgumentParserException e) {
      parser.handleError(e);
      return Optional.empty();
    }
  }

  public static void main(String[] args) {
    Optional<ServerConfig> config = parseConfig(args);
    if (!config.isPresent()) {
      System.exit(1);
    }
    final TcpServer server = new TcpServer(config.get());

    try {
      server.start();
    } catch (IOException e) {
      logger.error("Failed to start server", e);
      System.exit(1);
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutting down");
      try {
        server.close();
      } catch (IOException e) {
        logger.error("Failed to close server", e);
        System.exit(1);
      }
    }));
  }
}
