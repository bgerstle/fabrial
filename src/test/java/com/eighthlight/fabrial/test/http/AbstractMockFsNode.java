package com.eighthlight.fabrial.test.http;

import java.util.Objects;

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

  @Override
  public String toString() {
    return "AbstractMockFsNode{" +
           "name='" + name + '\'' +
           '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AbstractMockFsNode that = (AbstractMockFsNode) o;
    return Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
