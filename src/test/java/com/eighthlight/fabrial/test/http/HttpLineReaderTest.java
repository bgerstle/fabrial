package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.utils.HttpLineReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

import static com.eighthlight.fabrial.http.HttpConstants.CRLF;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.Generate.constant;
import static org.quicktheories.generators.SourceDSL.lists;
import static org.quicktheories.generators.SourceDSL.strings;

public class HttpLineReaderTest {
  @ParameterizedTest
  @ValueSource(strings = {"", CRLF})
  void returnsEmptyString(String empty) {
    var codePoints = empty.codePoints().toArray();
    var buffer = ByteBuffer.allocate(codePoints.length * 4);
    buffer.asIntBuffer().put(codePoints);
    var bais = new ByteArrayInputStream(buffer.array());
    var readBytes =
        Result.attempt(new HttpLineReader(bais)::readLine)
              .orElseAssert()
              .codePoints()
              .toArray();
    var readLine = new String(readBytes, 0, readBytes.length);
    assertThat(readLine, is(emptyString()));
  }

  @Test
  void returnsLineWithoutCRLF() {
    qt().forAll(strings().allPossible().ofLengthBetween(0, 32),
                constant(CRLF).toOptionals(20))
        .assuming((s, optCRLF) -> !s.contains(CRLF))
        .checkAssert((s, optCRLF) -> {
          var combined = optCRLF.map(c -> s + c).orElse(s);
          var codePoints = combined.codePoints().toArray();
          var buffer = ByteBuffer.allocate(codePoints.length * 4);
          buffer.asIntBuffer().put(codePoints);
          var bais = new ByteArrayInputStream(buffer.array());
          var readBytes =
              Result.attempt(new HttpLineReader(bais)::readLine)
                    .orElseAssert()
                    .codePoints()
                    .toArray();
          var readLine = new String(readBytes, 0, readBytes.length);
          assertThat(readLine, equalTo(s));
        });
  }

  @Test
  void readsLines() {
    qt().forAll(lists().of(strings().allPossible()
                                    .ofLengthBetween(0, 32))
                       .ofSizeBetween(0, 5))
        .checkAssert(lines -> {
          var joinedLines = String.join(CRLF, lines);
          var codePoints = joinedLines.codePoints().toArray();
          var buffer = ByteBuffer.allocate(codePoints.length * 4);
          buffer.asIntBuffer().put(codePoints);
          var bais = new ByteArrayInputStream(buffer.array());

          for (var line: lines) {
            var readBytes =
                Result.attempt(new HttpLineReader(bais)::readLine)
                      .orElseAssert()
                      .codePoints()
                      .toArray();
            var readLine = new String(readBytes, 0, readBytes.length);
            assertThat(readLine, equalTo(line));
          }
        });
  }
}
