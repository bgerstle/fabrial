package com.eighthlight.fabrial.test.client;

import com.bgerstle.result.Result;
import net.logstash.logback.argument.StructuredArguments;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Optional;

import static java.lang.Thread.sleep;

public class TcpClient implements AutoCloseable {
  public final SocketAddress address;
  private Socket socket;

  private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);

  public TcpClient(SocketAddress address) {
    this.address = address;
  }

  public void connect() throws IOException {
    connect(1000, 3, 1000);
  }

  public void connect(int timeout, int retries, int delay) throws IOException {
    socket = new Socket();

    Optional<IOException> ioe = Result.attempt(() -> socket.connect(address, timeout)).getError();
    if (!ioe.isPresent()) {
      return;
    }
    int remainingTries = retries - 1;
    if (remainingTries <= 0) {
      throw ioe.get();
    }

    logger.info("Retrying... {} (ms) {}",
                StructuredArguments.kv("delay", delay),
                StructuredArguments.kv("retries", remainingTries),
                ioe);
    try {
      sleep(delay);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    connect(timeout, retries - 1, delay);
  }

  public boolean isClosed() {
    assert socket != null;
    return socket.isClosed();
  }

  public OutputStream getOutputStream() throws IOException {
    assert socket != null;
    return socket.getOutputStream();
  }

  public InputStream getInputStream() throws IOException {
    assert socket != null;
    return socket.getInputStream();
  }

  @Override
  public void close() throws IOException {
    assert socket != null;
    socket.close();
  }
}
