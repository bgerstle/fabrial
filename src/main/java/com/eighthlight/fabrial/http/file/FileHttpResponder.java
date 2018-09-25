package com.eighthlight.fabrial.http.file;

import com.eighthlight.fabrial.http.HttpResponder;
import com.eighthlight.fabrial.http.HttpVersion;
import com.eighthlight.fabrial.http.Method;
import com.eighthlight.fabrial.http.request.HttpRequestByteRange;
import com.eighthlight.fabrial.http.request.Request;
import com.eighthlight.fabrial.http.response.Response;
import com.eighthlight.fabrial.http.response.ResponseBuilder;
import com.eighthlight.fabrial.utils.Result;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static java.lang.Math.toIntExact;

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

  private final FileController fileController;

  public interface FileController {
    boolean fileExistsAtPath(String relPathStr);

    boolean isDirectory(String relPathStr);

    List<String> getDirectoryContents(String relPathStr);

    long getFileSize(String relPathStr);

    String getFileMimeType(String relPathStr) throws IOException;

    InputStream getFileContents(String relPathStr, int offset, int length) throws IOException;

    void updateFileContents(String relPathStr, InputStream data, int length) throws IOException;

    void removeFile(String relPathStr) throws IOException;
  }

  public FileHttpResponder(FileController fileController) {
    this.fileController = fileController;
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
    if (!fileController.fileExistsAtPath(request.uri.getPath())
        && (request.method != Method.OPTIONS
            && request.method != Method.PUT)) {
      return builder.withStatusCode(404).build();
    }
    switch (request.method) {
      case HEAD: {
        return buildReadFileResponse(request, builder);
      }
      case GET: {
        return buildGetResponse(request, builder);
      }
      case OPTIONS: {
        return buildOptionsResponse(request, builder);
      }
      case PUT: {
        return buildPutResponse(request, builder);
      }
      case DELETE: {
        return buildDeleteResponse(request, builder);
      }
      default:
        return builder.withStatusCode(405).build();
    }
  }

  // serve "read" requests (GET/HEAD) for files. returning body if GET request
  private Response buildReadFileResponse(Request request, ResponseBuilder builder) {
    var size = fileController.getFileSize(request.uri.getPath());

    // set "default" status code of 200. logic below may override
    builder.withStatusCode(200);

    // exit early w/ "empty file" response
    if (size == 0L) {
      // size can also be 0 for absent files. assuming caller has checked existence already
      return builder.withHeader("Content-Length", "0")
                    .build();
    }

    try {
      var sizeStr = Long.toString(size);

      if (request.method.equals(Method.HEAD)) {
        builder.withHeader("Content-Length", sizeStr);
        builder.withHeader("Accept-Ranges", "bytes=0-" + Long.toString(size - 1));
      } else {
        var sizeInt = toIntExact(size);
        var rangeHeader = request.headers.get("Range");
        if (rangeHeader == null) {
          // requested entire file, skip range handling
          builder.withHeader("Content-Length", Long.toString(size))
                 .withBody(fileController.getFileContents(request.uri.getPath(),0, sizeInt));
        } else {
          HttpRequestByteRange range;
          try {
            range = HttpRequestByteRange.parseFromHeader(rangeHeader, sizeInt);
          } catch (HttpRequestByteRange.ParsingException e) {
            // return early with unsatisfiable range response
            return builder.withStatusCode(416)
                          .withHeader("Content-Range", "*/" + sizeInt)
                          .build();
          }

          builder.withHeader("Content-Length", Integer.toString(range.length()));
          builder.withHeader("Content-Range",
                             range.toString() + "/" + sizeInt);

          // TODO: wrap w/ try/catch for index out of bounds and return invalid range
          builder.withBody(fileController.getFileContents(request.uri.getPath(),
                                                          range.first,
                                                          range.length()));
          builder.withStatusCode(206);
        }
      }

      var mimeType = fileController.getFileMimeType(request.uri.getPath());
      if (mimeType != null) {
        builder.withHeader("Content-Type", mimeType);
      }

      return builder.build();
    } catch (FileNotFoundException e) {
      logger.warn("Unexpected FileNotFoundException getting contents for {}",
                  StructuredArguments.kv("request", request.uri.getPath()));
      // in the off chance this happens between the time we checked in getResponse & now, return 404
      return builder.withStatusCode(404).build();
    } catch (IOException e) {
      logger.warn("Failed to get contents of file for {}",
                   StructuredArguments.kv("request", request));
      // Some other exception, wrap in 500
      return builder.withStatusCode(500).withReason(e.getMessage()).build();
    }
  }

  private Response buildGetResponse(Request request, ResponseBuilder builder) {
    if (fileController.isDirectory(request.uri.getPath())) {
      return buildGetDirectoryResponse(request, builder);
    } else {
      return buildReadFileResponse(request, builder);
    }
  }

  private Response buildOptionsResponse(Request request, ResponseBuilder builder) {
    return builder.withStatusCode(200)
                  .withHeader("Allow", allowedMethodsAsString)
                  .withHeader("Content-Length", "0")
                  .build();
  }

  private Response buildGetDirectoryResponse(Request request, ResponseBuilder builder) {
    var contents =
        fileController.getDirectoryContents(request.uri.getPath())
                      .stream()
                      .sorted()
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

  private Response buildPutResponse(Request request, ResponseBuilder builder) {
    var contentLength = Optional
        .ofNullable(request.headers.get("Content-Length"))
        .map(l -> Result.attempt(() -> Integer.parseInt(l)));
    if (!contentLength.isPresent()) {
      return builder.withStatusCode(411).build();
    } else if (contentLength.get().getError().isPresent()) {
      logger.trace("Failed to parse content length");
      return builder.withStatusCode(411).build();
    }

    try {
      var didExist = fileController.fileExistsAtPath(request.uri.getPath());
      var unwrappedLength = contentLength.get().getValue().get();
      fileController.updateFileContents(request.uri.getPath(), request.body, unwrappedLength);
      return builder.withStatusCode(didExist ? 200 : 201).build();
    } catch (IOException e) {
      // e.g. trying to PUT a directory or some such nonsense
      return builder.withStatusCode(400).withReason(e.getMessage()).build();
    }
  }

  private Response buildDeleteResponse(Request request, ResponseBuilder builder) {
    try {
      fileController.removeFile(request.uri.getPath());
    } catch (IOException e) {
      return builder.withStatusCode(500)
                    .withReason(e.getMessage())
                    .build();
    }
    return builder.withStatusCode(200).build();
  }
}
