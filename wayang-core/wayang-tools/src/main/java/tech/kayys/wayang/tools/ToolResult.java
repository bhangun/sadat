
@Value
@Builder
public class ToolResult {
    String requestId;
    String toolId;
    Status status;
    Map<String, Object> output;
    Duration executionTime;
    Optional<String> error;
    Map<String, Object> metadata;
}