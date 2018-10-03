package com.eighthlight.fabrial.http.message;

import com.eighthlight.fabrial.utils.CheckedFunction;
import com.eighthlight.fabrial.utils.HttpLineReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class MessageReader<MessageT, BuilderT extends HttpMessageBuilder<BuilderT, MessageT>> {
  private final CheckedFunction<String, BuilderT, MessageReaderException> builderFactory;

  protected final InputStream is;

  public MessageReader(CheckedFunction<String, BuilderT, MessageReaderException> builderFactory, InputStream is) {
    this.builderFactory = builderFactory;
    this.is = is;
  }

  private BuilderT startBuilderWithLine(String line) throws MessageReaderException {
    return builderFactory.apply(line);
  }

  public Optional<MessageT> read() throws MessageReaderException {
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
