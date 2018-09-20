package com.eighthlight.fabrial.test.http;

public class MockFile extends AbstractMockFsNode {
  public String type;
  public byte[] data;

  public MockFile(String name) {
    super(name);
  }
}
