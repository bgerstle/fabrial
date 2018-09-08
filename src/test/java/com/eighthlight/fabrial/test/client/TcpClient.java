package com.eighthlight.fabrial.test.client;

import com.eighthlight.fabrial.utils.Result;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;

import static com.eighthlight.fabrial.utils.Result.attempt;
import static java.lang.Thread.sleep;

public class TcpClient implements AutoCloseable {
  public final SocketAddress address;
  private Socket socket;

  private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);

  public TcpClient(SocketAddress address) {
    this.address = address;
  }

  public void connect() throws IOException {
    connect(100, 0, 0);
  }

  public void connect(int timeout) throws IOException {
    connect(timeout, 0, 0);
  }

  public void connect(int timeout, int retries, int delay) throws IOException {
    socket = new Socket();

    Result<ObjectUtils.Null, IOException> result =
        attempt(() -> socket.connect(address, timeout));

    Optional<IOException> ioe = result.getError();
    if (!ioe.isPresent()) {
      return;
    }
    int remainingTries = retries - 1;
    if (remainingTries <= 0) {
      throw ioe.get();
    }

    logger.info("Failed to connect (" + ioe.get().getMessage() + "). Retrying in " + delay + "ms");
    try {
      sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    connect(timeout, retries - 1, delay);
  }

  public boolean isClosed() {
    return socket.isClosed();
  }

  public OutputStream getOutputStream() throws IOException {
    return socket.getOutputStream();
  }

  public InputStream getInputStream() throws IOException {
    return socket.getInputStream();
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }
}
