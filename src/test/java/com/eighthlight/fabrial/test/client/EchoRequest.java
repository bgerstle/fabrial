package com.eighthlight.fabrial.test.client;

import java.io.*;
import java.util.logging.Logger;

/**
 * Given client streams, send data to a server and read back the response.
 */
public class EchoRequest {
  OutputStream outputStream;
  InputStream inputStream;

  Logger logger;

  public EchoRequest(OutputStream outputStream,
                     InputStream inputStream) {
    this.outputStream= outputStream;
    this.inputStream = inputStream;
    this.logger = Logger.getLogger(this.toString());
  }

  /**
   * @param object The object to be serialized and sent to the server.
   *
   * @return An object which was de-serialized from the server response.
   */
  public <T extends Serializable> T send(T object) throws IOException, ClassNotFoundException {
    write(object);
    return read();
  }

  private <T extends Serializable> void write(T object) throws IOException {
    try (ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutput objectOut = new ObjectOutputStream(bytesOut)) {
      objectOut.writeObject(object);
      objectOut.flush();
      bytesOut.writeTo(outputStream);
      bytesOut.flush();
    }
  }

  private <T extends Serializable> T read() throws IOException, ClassNotFoundException {
    try (ObjectInputStream objectReader = new ObjectInputStream(inputStream)) {
      // If the server is writing back the data we wrote, the cast should succeed. Otherwise,
      // an exception will be thrown and the request will fail
      return (T) objectReader.readObject();
    }
  }
}
