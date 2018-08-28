import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConfigTest {
  @Test
  void defaultPort() {
    ServerConfig conf = new ServerConfig();
    assertThat(conf.port, equalTo(80));
  }

  @Test
  void customPort() {
    int port = 81;
    ServerConfig conf = new ServerConfig(port);
    assertThat(conf.port, equalTo(port));
  }

}
