@Value
@Builder
public class Implementation {
    ImplementationType type;
    String coordinate;     // maven:group:artifact:version or uri
    String digest;
    Map<String, String> metadata;
}