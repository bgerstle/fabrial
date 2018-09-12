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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class FileHttpResponder implements HttpResponder {
  private static final Logger logger = LoggerFactory.getLogger(FileHttpResponder.class);

  private final DataSource dataSource;

  public static interface DataSource {
    public boolean fileExistsAtPath(Path path);

    public boolean isDirectory(Path path);

    public List<Path> getDirectoryContents(Path path);
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
    Path requestPath = Paths.get(request.uri.getPath());
    boolean fileExists = dataSource.fileExistsAtPath(requestPath);
    final var builder = new ResponseBuilder().withVersion(HttpVersion.ONE_ONE);
    if (!fileExists && request.method != Method.OPTIONS) {
      return builder.withStatusCode(404).build();
    }
    switch (request.method) {
      case HEAD: {
        return builder.withStatusCode(200).build();
      }
      case GET: {
        if (dataSource.isDirectory(requestPath)) {
          var contents =
              dataSource.getDirectoryContents(requestPath)
                        .stream()
                        .sorted()
                        .map(p -> p.getFileName().toString())
                        .reduce((p1, p2) -> p1 + "," + p2)
                        .orElse("");
          var charset = StandardCharsets.UTF_8;
          var contentBytes = contents.getBytes(charset);
          return builder.withStatusCode(200)
              .withHeader("Content-Type", "text/plain; charset=" + charset.name().toLowerCase())
              .withHeader("Content-Length", Integer.toString(contentBytes.length))
              .withBody(new ByteArrayInputStream(contentBytes))
              .build();
        } else {
          return builder.withStatusCode(501).withReason("coming soon!").build();
        }
      }
      case OPTIONS: {
        String allowedMethods =
            List.of(Method.GET,
                    Method.HEAD,
                    Method.OPTIONS,
                    Method.PUT,
                    Method.DELETE)
                .stream()
                .map(Method::name)
                .reduce((m, s) -> s + ", " + m)
                .get();
        return builder.withStatusCode(200)
                      .withHeaders(Map.of("Allow", allowedMethods,
                                          "Content-Length", "0"))
                      .build();
      }
      default:
        return builder.withStatusCode(501).build();
    }
  }
}
