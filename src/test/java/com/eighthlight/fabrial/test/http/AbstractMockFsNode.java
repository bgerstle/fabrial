package com.eighthlight.fabrial.test.http;

public abstract class AbstractMockFsNode implements MockFsNode {
  public String name;

  public AbstractMockFsNode(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }
}
