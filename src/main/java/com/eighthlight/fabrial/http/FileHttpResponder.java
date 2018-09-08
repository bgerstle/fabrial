package com.eighthlight.fabrial.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHttpResponder implements HttpResponder {
  private static final Logger logger = LoggerFactory.getLogger(FileHttpResponder.class);
  private final DataSource dataSource;

  public static interface DataSource {
    public boolean fileExistsAtPath(Path path);
  }

  public FileHttpResponder(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean matches(Request request) {
    // ideally this would have its own namespace, like `/static/*`
    return true;
  }

  @Override
  public Response getResponse(Request request) {
    // FIXME: should be 501 for unsupported methods on existing files
    if (!request.method.equals(Method.HEAD)) {
      return new Response(HttpVersion.ONE_ONE, 501, null);
    }
    boolean fileExists = dataSource.fileExistsAtPath(Paths.get(request.uri.getPath()));
    int statusCode = fileExists ? 200 : 404;
    return new Response(HttpVersion.ONE_ONE, statusCode, null);
  }
}
