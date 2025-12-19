
@Value
@Builder
public class WorkflowVersion {
    UUID id;
    UUID workflowId;
    String version;
    WorkflowDefinition snapshot;
    String createdBy;
    Instant createdAt;
    List<String> tags;
}
