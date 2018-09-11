package com.eighthlight.fabrial.http;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    if (!fileExists) {
      return new Response(HttpVersion.ONE_ONE, 404, null);
    }
    if (!request.method.equals(Method.HEAD)) {
      return new Response(HttpVersion.ONE_ONE, 501, null);
    }
    return new Response(HttpVersion.ONE_ONE, 200, null);
  }
}
