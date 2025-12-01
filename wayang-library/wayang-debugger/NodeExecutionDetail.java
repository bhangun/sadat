
@Value
@Builder
public class NodeExecutionDetail {
    String nodeId;
    NodeState nodeState;
    ProvenanceRecord provenance;
    List<LogEntry> logs;
    Object inputs;
    Object outputs;
    ExecutionMetrics metrics;
    Optional<ReasoningTrace> reasoningTrace;
}