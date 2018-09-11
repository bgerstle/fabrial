package com.eighthlight.fabrial.test.http;

import com.eighthlight.fabrial.http.HttpHeaderWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class HttpHeaderWriterSerializationTests {
  @Test
  void allowHEAD() throws IOException {
    try (var os = new ByteArrayOutputStream()) {
      var writer = new HttpHeaderWriter(os);
      writer.writeField("Allow", "GET");
      assertThat(os.toString(), is("Allow: GET"));
    }
  }
}
