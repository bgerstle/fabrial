package com.eighthlight.fabrial.http;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class FileHttpResponder implements HttpResponder {
  private static final Logger logger = Logger.getLogger(FileHttpResponder.class.getName());
  private final FileResponderDataSource dataSource;

  public static interface FileResponderDataSource {
    public boolean fileExistsAtPath(Path path);
  }

  public FileHttpResponder(FileResponderDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean matches(Request request) {
    // ideally this would have its own namespace, like `/static/*`
    return true;
  }

  @Override
  public Response getResponse(Request request) {
    if (!request.method.equals(Method.HEAD)) {
      return new Response(HttpVersion.ONE_ONE, 501, null);
    }
    boolean fileExists = dataSource.fileExistsAtPath(Paths.get(request.uri.getPath()));
    int statusCode = fileExists ? 200 : 404;
    return new Response(HttpVersion.ONE_ONE, statusCode, null);
  }
}
