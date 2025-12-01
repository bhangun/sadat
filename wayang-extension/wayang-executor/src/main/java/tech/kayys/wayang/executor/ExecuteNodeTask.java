@Value
@Builder
public class ExecuteNodeTask {
    String taskId;
    UUID runId;
    UUID workflowId;
    String nodeId;
    NodeDescriptor nodeDescriptor;
    Map<String, Object> inputPayload;
    TaskMetadata metadata;
    Duration deadline;
}
