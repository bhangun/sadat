@Value
@Builder
public class ToolDescriptor {
    String id;
    String name;
    String version;
    String description;
    ToolType type;
    JsonSchema inputSchema;
    JsonSchema outputSchema;
    List<String> requiredSecrets;
    Set<Capability> capabilities;
    ToolEndpoint endpoint;
    RateLimitConfig rateLimitConfig;
    Duration timeout;
    RetryConfig retryConfig;
    Map<String, Object> metadata;
}
