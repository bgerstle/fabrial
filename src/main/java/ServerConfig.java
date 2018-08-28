import org.immutables.value.Value;

@Value.Immutable
public abstract class ServerConfig {

  @Value.Default
  public int port() {
    return 80; }
}
