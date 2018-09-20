package com.eighthlight.fabrial.test.http;

import java.util.Arrays;
import java.util.Objects;

public class MockFile extends AbstractMockFsNode {
  public String type;
  public byte[] data;

  public MockFile(String name) {
    super(name);
  }

  @Override
  public String toString() {
    return "MockFile{" +
           "name='" + name + "'" +
           "type='" + type + '\'' +
           ", data=" + Arrays.toString(data) +
           '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    MockFile mockFile = (MockFile) o;
    return Objects.equals(type, mockFile.type) &&
           Arrays.equals(data, mockFile.data);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), type);
    result = 31 * result + Arrays.hashCode(data);
    return result;
  }
}
