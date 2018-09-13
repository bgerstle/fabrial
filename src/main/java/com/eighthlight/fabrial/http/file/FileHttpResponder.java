package com.eighthlight.fabrial.http.file;

import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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

    Long getFileSize(Path path);

    InputStream getFileContents(Path path);
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
      return builder.withStatusCode(501).withReason("coming soon!").build();
    }
  }

  private Response buildOptionsResponse(Request requet, ResponseBuilder builder) {
    return builder.withStatusCode(200)
                  .withHeader("Allow", allowedMethodsAsString)
                  .withHeader("Content-Length", "0")
                  .build();
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
