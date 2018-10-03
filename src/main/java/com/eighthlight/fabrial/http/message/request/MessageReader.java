package com.eighthlight.fabrial.http.message.request;

import com.eighthlight.fabrial.http.message.HttpHeaderReader;
import com.eighthlight.fabrial.http.message.HttpMessageBuilder;
import com.eighthlight.fabrial.http.message.MessageReaderException;
import com.eighthlight.fabrial.utils.CheckedFunction;
import com.eighthlight.fabrial.utils.HttpLineReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class MessageReader<M, B extends HttpMessageBuilder<B, M>> {
  private final CheckedFunction<String, B, MessageReaderException> builderFactory;

  protected final InputStream is;

  public MessageReader(CheckedFunction<String, B, MessageReaderException> builderFactory, InputStream is) {
    this.builderFactory = builderFactory;
    this.is = is;
  }

  private B startBuilderWithLine(String line) throws MessageReaderException {
    return builderFactory.apply(line);
  }

  public Optional<M> read() throws MessageReaderException {
    try {
      var lineReader = new HttpLineReader(is);
      var firstLine = lineReader.readLine();
      if (firstLine == null || firstLine.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(
          startBuilderWithLine(firstLine)
              .withHeaders(new HttpHeaderReader(is).readHeaders())
              .withBody(is)
              .build()
      );
    } catch (IOException | IllegalArgumentException e) {
      // ???: maybe better to separate IO from parsing, but need more refactoring
      // Catch any input validation  or IO errors in `build`'s invocation of target type constructor
      throw new MessageReaderException(e);
    }
  }
}
