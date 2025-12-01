
@Value
@Builder
public class ToolRequest {
    String requestId;
    String toolId;
    Map<String, Object> parameters;
    UUID tenantId;
    UUID runId;
    String nodeId;
    Duration timeout;
    Map<String, String> metadata;
}