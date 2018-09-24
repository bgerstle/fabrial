package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.file.FileHttpResponder;
import com.eighthlight.fabrial.utils.Result;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.eighthlight.fabrial.test.http.MockDirectory.PATH_SEPARATOR;

public class MockFileController implements FileHttpResponder.FileController {
  MockDirectory root;

  private Optional<MockFsNode> fileAtPath(String relPathStr) {
    return relPathStr.matches("/?") ?
        Optional.of(root) : root.findChild(relPathStr);
  }

  @Override
  public boolean fileExistsAtPath(String relPathStr) {
    return fileAtPath(relPathStr).isPresent();
  }

  @Override
  public boolean isDirectory(String relPathStr) {
    return false;
  }

  @Override
  public List<String> getDirectoryContents(String relPathStr) {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.of((MockDirectory)f) : Optional.empty())
        .map(d -> d.children)
        .map(cs -> cs.stream().map(c -> c.getName()).collect(Collectors.toList()))
        .orElse(List.of());
  }

  @Override
  public long getFileSize(String relPathStr) {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .map(f -> f.data == null ? 0 : f.data.length)
        .map(Integer::toUnsignedLong)
        .orElse(0L);
  }

  @Override
  public String getFileMimeType(String relPathStr) throws IOException {
    var file = fileAtPath(relPathStr);
    return file
        .flatMap(f -> f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .map(f -> f.type)
        .orElse(null);
  }

  @Override
  public InputStream getFileContents(String relPathStr, int offset, int length) throws IOException {
    var file = fileAtPath(relPathStr);
    if (!file.isPresent()) {
      throw new FileNotFoundException("File not found");
    }
    var lastIndex = offset+length-1;
    if (offset < 0 || lastIndex > getFileSize(relPathStr)) {
      throw new IOException("Read out of bounds");
    }
    return file
        .flatMap(f -> f.isDirectory() ? Optional.empty() : Optional.of((MockFile)f))
        .map(f -> Arrays.copyOfRange(f.data, offset, lastIndex))
        .map(ByteArrayInputStream::new)
        .orElse(null);
  }

  @Override
  public void updateFileContents(String relPathStr, InputStream data, int length) throws IOException {
    var node = fileAtPath(relPathStr);
    if (node.isPresent() && node.get().isDirectory()) {
      throw new FileNotFoundException("Can't update file data of a directory");
    }
    var pathComponents = Arrays.asList(relPathStr.split(PATH_SEPARATOR));
    if (pathComponents.size() == 0) {
      throw new FileNotFoundException("Can't update file data at empty path");
    }

    var file = (MockFile)node
        .map(Result::<MockFile, IOException>of)
        .orElseGet(() -> Result.attempt(() -> {
          var name = pathComponents.get(pathComponents.size()-1);
          var newFile = new MockFile(name);
          var parentPath = String.join(PATH_SEPARATOR, pathComponents.subList(0, pathComponents.size()-1));
          var parent = fileAtPath(parentPath);
          if (!parent.isPresent()) {
            throw new FileNotFoundException("Parent doesn't exist");
          } else if (!parent.get().isDirectory()){
            throw new FileNotFoundException("File exists at parent path");
          }
          var parentDir = (MockDirectory)parent.get();
          parentDir.children.add(newFile);
          return newFile;
        }))
        .orElseThrow();

    var buf = ByteBuffer.allocate(length);
    data.read(buf.array(), 0, length);
    file.data = buf.array();
  }

  @Override
  public void removeFile(String relPathStr) throws IOException {
    var file = fileAtPath(relPathStr);
    if (!file.isPresent()) {
      throw new FileNotFoundException("No file at path " + relPathStr);
    }

    if (file.get().isDirectory()
        && !((MockDirectory)file.get()).children.isEmpty()) {
      throw new IOException("Directory not empty");
    }

    var parentPath = Optional.ofNullable(Paths.get(relPathStr).getParent())
                             .map(Path::toString)
                             .orElse("");
    var parent =
        fileAtPath(parentPath)
            .flatMap(p -> p.isDirectory() ? Optional.of((MockDirectory)p) : Optional.empty());
    if (!parent.isPresent()) {
      throw new FileNotFoundException("Parent not found at " + parentPath);
    }
    parent.get().children.remove(file.get());
  }
}
