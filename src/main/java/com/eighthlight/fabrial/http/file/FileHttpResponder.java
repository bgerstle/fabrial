package com.eighthlight.fabrial.http.file;

import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class FileHttpResponder implements HttpResponder {
  private final DataSource dataSource;

  public static interface DataSource {
    public boolean fileExistsAtPath(Path path);
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
    boolean fileExists = dataSource.fileExistsAtPath(Paths.get(request.uri.getPath()));
    final var builder = new ResponseBuilder().withVersion(HttpVersion.ONE_ONE);
    if (!fileExists && request.method != Method.OPTIONS) {
      return builder.withStatusCode(404).build();
    }
    switch (request.method) {
      case HEAD:
        return builder.withStatusCode(200).build();
      case OPTIONS:
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
                                          "Content-Length","0"))
                      .build();
      default:
        return builder.withStatusCode(501).build();
    }
  }
}
