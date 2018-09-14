package com.eighthlight.fabrial.http.file;

import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseBuilder;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileHttpResponder implements HttpResponder {
  private static final Logger logger = LoggerFactory.getLogger(FileHttpResponder.class);

  private static final List<Method> allowedMethods  = List.of(Method.GET,
                                                              Method.HEAD,
                                                              Method.OPTIONS,
                                                              Method.PUT,
                                                              Method.DELETE);

  private static final String allowedMethodsAsString = allowedMethods.stream()
                                                                     .map(Method::name)
                                                                     .reduce((m, s) -> s + ", " + m)
                                                                     .get();

  private final DataSource dataSource;

  public interface DataSource {
    boolean fileExistsAtPath(Path path);

    boolean isDirectory(Path path);

    List<Path> getDirectoryContents(Path path);

    long getFileSize(Path path);

    InputStream getFileContents(Path path) throws IOException;
  }

  public FileHttpResponder(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean matches(Request request) {
    // ideally this would have its own namespace, like `/static/*` or `/files/*`
    return true;
  }

  @Override
  public Response getResponse(Request request) {
    final var builder = new ResponseBuilder().withVersion(HttpVersion.ONE_ONE);
    // bail early if file doesn't exist and this isn't an OPTIONS request
    if (!dataSource.fileExistsAtPath(Paths.get(request.uri.getPath()))
        && request.method != Method.OPTIONS) {
      return builder.withStatusCode(404).build();
    }
    switch (request.method) {
      case HEAD: {
        return buildHeadResponse(request, builder);
      }
      case GET: {
        return buildGetResponse(request, builder);
      }
      case OPTIONS: {
        return buildOptionsResponse(request, builder);
      }
      default:
        return builder.withStatusCode(501).build();
    }
  }

  private Response buildHeadResponse(Request request, ResponseBuilder builder) {
    // TODO: add headers for based on file:
    // Content-Length, Content-Type, Content-Range
    return builder.withStatusCode(200).build();
  }

  private Response buildGetResponse(Request request, ResponseBuilder builder) {
    if (dataSource.isDirectory(Paths.get(request.uri.getPath()))) {
      return buildGetDirectoryResponse(request, builder);
    } else {
      return buildGetFileResponse(request, builder);
    }
  }

  private Response buildOptionsResponse(Request requet, ResponseBuilder builder) {
    return builder.withStatusCode(200)
                  .withHeader("Allow", allowedMethodsAsString)
                  .withHeader("Content-Length", "0")
                  .build();
  }

  private Response buildGetFileResponse(Request request, ResponseBuilder builder) {
    var size = dataSource.getFileSize(Paths.get(request.uri.getPath()));
    if (size == 0L) {
      // size can also be 0 for absent files. assuming caller has checked existence already
      return builder.withStatusCode(200)
                    .withHeader("Content-Length", Long.toString(size))
                    .build();
    }
    try {
      return builder.withStatusCode(200)
                    // TODO: add content-type
                    .withBody(dataSource.getFileContents(Paths.get(request.uri.getPath())))
                    .withHeader("Content-Length", Long.toString(size))
                    .build();
    } catch (FileNotFoundException e) {
      logger.warn("Unexpected FileNotFoundException getting contents for {}",
                  StructuredArguments.kv("request", request.uri.getPath()));
      // in the off chance this happens between the time we checked in getResponse & now, return 404
      return builder.withStatusCode(404).build();
    } catch (IOException e) {
      logger.error("Unexpected IOException getting contents of file for {}",
                  StructuredArguments.kv("request", request));
      // Some other exception, wrap in 500
      return builder.withStatusCode(500).withReason(e.getMessage()).build();
    }
  }

  private Response buildGetDirectoryResponse(Request request, ResponseBuilder builder) {
    var contents =
        dataSource.getDirectoryContents(Paths.get(request.uri.getPath()))
                  .stream()
                  .sorted()
                  .map(p -> p.getFileName().toString())
                  .reduce((p1, p2) -> p1 + "," + p2)
                  .orElse("");

    if (contents.isEmpty()) {
      return builder.withHeader("Content-Length", "0")
                    .withStatusCode(200)
                    .build();
    }

    var charset = StandardCharsets.UTF_8;
    var contentBytes = contents.getBytes(charset);
    return builder.withStatusCode(200)
                  .withHeader("Content-Length", Integer.toString(contentBytes.length))
                  .withHeader("Content-Type", "text/plain; charset=" + charset.name().toLowerCase())
                  .withBody(new ByteArrayInputStream(contentBytes))
                  .build();
  }
}
