
@Value
@Builder
public class LLMRequest {
    String requestId;
    UUID tenantId;
    UUID runId;
    String nodeId;
    LLMRequestType type;
    Prompt prompt;
    Set<String> capabilities;
    Duration latencyBudget;
    Integer maxTokens;
    boolean stream;
    boolean cacheable;
    Map<String, Object> metadata;
}
