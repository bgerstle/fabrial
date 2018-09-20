package com.eighthlight.fabrial.test.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MockDirectory extends AbstractMockFsNode {
  private static final String PATH_SEPARATOR = "/";
  public ArrayList<MockFsNode> children;


  public MockDirectory(String name) {
    this(name, List.of());
  }

  public MockDirectory(String name, List<MockFsNode> children) {
    super(name);
    this.children = new ArrayList<>(children);
  }

  public Optional<MockFsNode> findChild(String path) {
    return findChild(Arrays.asList(path.split(PATH_SEPARATOR)));
  }

  public Optional<MockFsNode> findChild(List<String> pathComponents) {
    if (pathComponents.isEmpty()) {
      return Optional.empty();
    }
    var matchingChild = children.stream()
                                .filter(n -> n.getName().equals(pathComponents.get(0)))
                                .findFirst();
    if (pathComponents.size() == 1) {
      return matchingChild;
    }
    return matchingChild
        .flatMap(c -> c instanceof MockDirectory ? Optional.of(c) : Optional.empty())
        .flatMap(d -> {
          var matchingDir = (MockDirectory)matchingChild.get();
          var subComponents = pathComponents.subList(1, pathComponents.size());
          return matchingDir.findChild(subComponents);
        });
  }

  @Override
  public boolean isDirectory() {
    return true;
  }
}
