package com.eighthlight.fabrial.test.http.responder;

import com.eighthlight.fabrial.http.message.response.Response;
import com.eighthlight.fabrial.http.message.response.ResponseBuilder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.eighthlight.fabrial.test.gen.ArbitraryHttp.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.quicktheories.QuickTheory.qt;

public class ResponseBuilderTest {
  @Test
  public void generatesRequestsFromExpectedComponents() {
    qt().forAll(httpVersions(),
                responseReasons(10).toOptionals(50),
                statusCodes(),
                headers().toOptionals(50))
        .checkAssert((version, reason, statusCode, headers) -> {
      var body = new ByteArrayInputStream(new byte[]{});

      var response = new ResponseBuilder()
          .withVersion(version)
          .withStatusCode(statusCode)
          .withReason(reason.orElse(null))
          .withHeaders(headers.orElse(null))
          .withBody(body)
          .build();

      assertThat(response.statusCode, equalTo(statusCode));

      assertThat(response.reason, equalTo(reason.orElse(null)));

      if (headers.isPresent()) {
        assertThat(response.headers, equalTo(headers.get()));
      } else {
        assertThat(response.headers.isEmpty(), is(true));
      }

      assertThat(response.body, is(body));
    });
  }

  @Test
  public void generatesEquivalentResponseWhenInitializedWithResponse() {
    qt().forAll(httpVersions(),
                responseReasons(10).toOptionals(50),
                statusCodes(),
                headers().toOptionals(50))
        .checkAssert((version, reason, statusCode, headers) -> {
          var body = new ByteArrayInputStream(new byte[]{});

          var response = new Response(version,
                                      statusCode,
                                      reason.orElse(null),
                                      headers.orElse(null),
                                      body);

          assertThat(new ResponseBuilder(response).build(), equalTo(response));
        });
  }
}
