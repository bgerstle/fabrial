package com.eighthlight.fabrial.test.http.client;

import com.eighthlight.fabrial.http.message.MessageReaderException;
import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.http.request.RequestWriter;

import java.io.IOException;
import java.util.Optional;

public class HttpClient implements AutoCloseable {
  private final TcpClient client;

  public HttpClient(TcpClient client) {
    this.client = client;
  }

  public Optional<Response> send(Request request) throws IOException, MessageReaderException {
    new RequestWriter(client.getOutputStream()).writeRequest(request);
    return new ResponseReader(client.getInputStream()).read();
  }

  @Override
  public void close() throws Exception {
    client.close();
  }
}
