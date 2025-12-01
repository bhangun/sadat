
@Builder
public class DefaultNodeContext implements NodeContext {
    private final String runId;
    private final String nodeId;
    private final String tenantId;
    private final String userId;
    private final String traceId;
    private final Map<String, Object> variables;
    private final Map<String, Object> inputs;
    private final Map<String, Object> outputs;
    private final ExecutionMetadata metadata;
    private final ProvenanceContext provenance;
    private final SecurityContext securityContext;
    
    // Implementation of all interface methods
}
