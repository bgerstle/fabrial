package com.eighthlight.fabrial.test.http.client;

import com.eighthlight.fabrial.http.message.request.Request;
import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.test.client.TcpClient;
import com.eighthlight.fabrial.test.http.request.RequestWriter;

import java.io.IOException;

public class HttpClient {
  private final TcpClient client;

  public HttpClient(TcpClient client) {
    this.client = client;
  }

  public Response send(Request request) throws IOException {
    new RequestWriter(client.getOutputStream()).writeRequest(request);
    return null;
  }
}
