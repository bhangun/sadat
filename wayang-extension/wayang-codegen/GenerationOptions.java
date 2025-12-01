@Value
@Builder
public class GenerationOptions {
    boolean includeObservability;
    boolean includeCache;
    boolean minimalRuntime;
    String javaVersion;
    List<String> additionalDependencies;
    Map<String, String> configuration;
}
