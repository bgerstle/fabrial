import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConfigTest {
  @Test
  void defautPort() {
    ServerConfig conf = ImmutableServerConfig.builder().build();
    assertThat(conf.port(), equalTo(80));
  }

  @Test
  void customPort() {
    int port = 81;
    ServerConfig conf = ImmutableServerConfig.builder().port(port).build();
    assertThat(conf.port(), equalTo(port));
  }

}
