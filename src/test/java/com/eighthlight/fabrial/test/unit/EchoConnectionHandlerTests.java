package com.eighthlight.fabrial.test.unit;

import com.eighthlight.fabrial.EchoConnectionHandler;
import com.eighthlight.fabrial.test.mocks.MockClientConnection;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class EchoConnectionHandlerTests {
  @Test
  void whenInputHasData_thenSameDataWrittenToOutput() {
    var mockInputData = "foo".getBytes();
    var mockConnection = new MockClientConnection();
    mockConnection.outputStream = new ByteArrayOutputStream();
    mockConnection.inputStream = new ByteArrayInputStream(mockInputData);

    var echoHandler = new EchoConnectionHandler();

    echoHandler.handle(mockConnection);

    assertArrayEquals(mockInputData, mockConnection.outputStream.toByteArray());
  }
}
