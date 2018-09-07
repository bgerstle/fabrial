package com.eighthlight.fabrial.http;

import java.nio.file.Path;

public class FileHttpResponder implements HttpResponder {
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
    return new Response(HttpVersion.ONE_ONE, 501, null);
  }
}
