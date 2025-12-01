
@Value
@Builder
public class TaskMetadata {
    UUID tenantId;
    String userId;
    String traceId;
    int priority;
    Map<String, String> tags;
}