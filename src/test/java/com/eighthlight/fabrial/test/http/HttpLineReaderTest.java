package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.utils.HttpLineReader;
import com.eighthlight.fabrial.utils.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.quicktheories.core.Gen;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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
  private static ByteArrayInputStream asAsciiByteStream(String string) {
    return new ByteArrayInputStream(string.getBytes(StandardCharsets.US_ASCII));
  }

  private static Gen<String> asciiStringsWithoutCRLF() {
    return strings().ascii().ofLengthBetween(0, 32).assuming(s -> !s.contains(CRLF));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", CRLF})
  void returnsEmptyString(String empty) {
    var readLine =
        Result.attempt(new HttpLineReader(asAsciiByteStream(empty))::readLine)
              .orElseAssert();
    assertThat(readLine, is(emptyString()));
  }

  @Test
  void returnsLineWithoutCRLF() {
    qt().forAll(asciiStringsWithoutCRLF(),
                constant(CRLF).toOptionals(20))
        .checkAssert((s, optCRLF) -> {
          var readLine =
              Result.attempt(new HttpLineReader(asAsciiByteStream(s))::readLine)
                    .orElseAssert();
          assertThat(readLine, equalTo(s));
        });
  }

  @Test
  void readsLines() {
    var listsOfAsciiStrings =
        lists().of(asciiStringsWithoutCRLF()).ofSizeBetween(0, 5);

    qt().forAll(listsOfAsciiStrings).checkAssert(lines -> {
          var joinedLines = String.join(CRLF, lines);
          var lineReader = new HttpLineReader(asAsciiByteStream(joinedLines));
          for (var line: lines) {
            var readLine = Result.attempt(lineReader::readLine).orElseAssert();
            assertThat(readLine, equalTo(line));
          }
          var readLineAfterExhaustedInput = Result.attempt(lineReader::readLine).orElseAssert();
          assertThat(readLineAfterExhaustedInput, is(emptyString()));
        });
  }
}
