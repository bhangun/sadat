package tech.kayys.wayang.node.model;

import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.common.spi.NodeContext;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.schema.node.EdgeDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.node.PortDescriptor;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.api.model.RunStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ExecutionContext - Immutable snapshot of workflow execution state.
 * 
 * This class maintains the complete state during workflow execution:
 * - Node execution results and outputs
 * - Intermediate data flow between nodes
 * - HITL suspension state
 * - Error history and retry metadata
 * - Timing and performance data
 * 
 * Design Principles:
 * - Thread-safe for concurrent node execution
 * - Serializable for state persistence
 * - Contains only deterministic data (no callbacks or executors)
 * - Supports checkpoint/resume for crash recovery
 */
@Data
@Builder(toBuilder = true)
public class ExecutionContext {

    private final String runId;
    private final String workflowId;
    private final String tenantId;
    private final WorkflowDefinition workflow;
    private final Instant startTime;

    // Input/Output state
    private final Map<String, Object> initialInputs;
    @Builder.Default
    private final Map<String, Object> outputs = new ConcurrentHashMap<>();

    // Node execution tracking
    @Builder.Default
    private final Map<String, NodeExecutionResult> nodeResults = new ConcurrentHashMap<>();

    @Builder.Default
    private final Map<String, NodeState> nodeStates = new ConcurrentHashMap<>();

    // Data flow between nodes (keyed by edge id or port mapping)
    @Builder.Default
    private final Map<String, Object> dataFlow = new ConcurrentHashMap<>();

    // Error tracking
    @Builder.Default
    private final List<ErrorPayload> errorHistory = new ArrayList<>();

    // HITL state
    private volatile boolean awaitingHuman;
    private volatile String humanTaskId;
    @Builder.Default
    private final List<HTILTaskResult> humanDecisions = new ArrayList<>();

    // Metadata for extensions (agents, memory, etc.)
    @Builder.Default
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    // Performance tracking
    @Builder.Default
    private final Map<String, Long> nodeDurations = new ConcurrentHashMap<>();

    /**
     * Create initial execution context from workflow run.
     */
    public static ExecutionContext create(WorkflowRun run, WorkflowDefinition workflow) {
        return ExecutionContext.builder()
                .runId(run.getId())
                .workflowId(workflow.getId())
                .tenantId(run.getTenantId())
                .workflow(workflow)
                .startTime(run.getStartTime())
                .initialInputs(run.getInputs())
                .build();
    }

    /**
     * Restore execution context from persisted workflow run.
     * Used for resume operations.
     */
    public static ExecutionContext restore(WorkflowRun run) {
        ExecutionContext context = ExecutionContext.builder()
                .runId(run.getId())
                .workflowId(run.getWorkflowId())
                .tenantId(run.getTenantId())
                .startTime(run.getStartTime())
                .initialInputs(run.getInputs())
                .awaitingHuman(run.getStatus() == RunStatus.AWAITING_HITL)
                .build();

        // Restore node states from checkpoint
        if (run.getCheckpoint() != null) {
            context.restoreFromCheckpoint(run.getCheckpoint());
        }

        return context;
    }

    /**
     * Create node-specific context for execution.
     */
    public NodeContext createNodeContext(NodeDefinition nodeDef) {
        return NodeContext.builder()
                .nodeId(nodeDef.getId())
                .runId(runId)
                .tenantId(tenantId)
                .inputs(resolveNodeInputs(nodeDef))
                .metadata(new HashMap<>(metadata))
                .workflow(workflow)
                .build();
    }

    /**
     * Resolve inputs for a node from previous node outputs and data flow.
     */
    private Map<String, Object> resolveNodeInputs(NodeDefinition nodeDef) {
        Map<String, Object> inputs = new HashMap<>();

        // Get incoming edges
        List<EdgeDefinition> incomingEdges = workflow.getEdges().stream()
                .filter(edge -> edge.getTo().equals(nodeDef.getId()))
                .toList();

        for (EdgeDefinition edge : incomingEdges) {
            // Get output from source node
            NodeExecutionResult sourceResult = nodeResults.get(edge.getFrom());
            if (sourceResult != null && sourceResult.isSuccess()) {
                Object outputData = sourceResult.getOutputChannels().get(edge.getFromPort());
                if (outputData != null) {
                    // Map to target input port
                    inputs.put(edge.getToPort(), outputData);
                }
            }
        }

        // Add data from data flow (for complex mappings)
        for (PortDescriptor input : nodeDef.getInputs()) {
            String key = nodeDef.getId() + "." + input.getName();
            if (dataFlow.containsKey(key)) {
                inputs.put(input.getName(), dataFlow.get(key));
            }
        }

        // Fallback to initial inputs for entry nodes
        if (inputs.isEmpty() && isEntryNode(nodeDef)) {
            inputs.putAll(initialInputs);
        }

        return inputs;
    }

    private boolean isEntryNode(NodeDefinition nodeDef) {
        return workflow.getEdges().stream()
                .noneMatch(edge -> edge.getTo().equals(nodeDef.getId()));
    }

    /**
     * Add node execution result to context.
     */
    public void addNodeResult(String nodeId, NodeExecutionResult result) {
        nodeResults.put(nodeId, result);
        nodeStates.put(nodeId, NodeState.fromResult(result));

        if (result.isError()) {
            errorHistory.add(result.getError());
        }

        // Store outputs in data flow for downstream nodes
        result.getOutputChannels().forEach((channel, data) -> {
            String key = nodeId + "." + channel;
            dataFlow.put(key, data);
        });
    }

    /**
     * Get output from a specific node.
     */
    public Optional<Object> getNodeOutput(String nodeId, String outputPort) {
        String key = nodeId + "." + outputPort;
        return Optional.ofNullable(dataFlow.get(key));
    }

    /**
     * Check if node has been executed.
     */
    public boolean hasNodeExecuted(String nodeId) {
        return nodeResults.containsKey(nodeId);
    }

    /**
     * Get node execution state.
     */
    public NodeState getNodeState(String nodeId) {
        return nodeStates.getOrDefault(nodeId, NodeState.PENDING);
    }

    /**
     * Apply corrected input after HITL decision.
     */
    public void applyCorrectedInput(String nodeId, Map<String, Object> correctedInput) {
        correctedInput.forEach((key, value) -> {
            String dataKey = nodeId + "." + key;
            dataFlow.put(dataKey, value);
        });
    }

    /**
     * Add human decision to history.
     */
    public void addHumanDecision(HTILTaskResult decision) {
        humanDecisions.add(decision);
    }

    /**
     * Add metadata entry.
     */
    public void addMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    /**
     * Get metadata value.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    /**
     * Calculate total execution duration.
     */
    public Duration getTotalDuration() {
        if (startTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Get duration for specific node.
     */
    public Duration getNodeDuration(String nodeId) {
        Long nanos = nodeDurations.get(nodeId);
        return nanos != null ? Duration.ofNanos(nanos) : Duration.ZERO;
    }

    /**
     * Record node execution duration.
     */
    public void recordNodeDuration(String nodeId, long nanos) {
        nodeDurations.put(nodeId, nanos);
    }

    /**
     * Create checkpoint for persistence.
     */
    public CheckpointData createCheckpoint() {
        return CheckpointData.builder()
                .nodeResults(new HashMap<>(nodeResults))
                .nodeStates(new HashMap<>(nodeStates))
                .dataFlow(new HashMap<>(dataFlow))
                .errorHistory(new ArrayList<>(errorHistory))
                .humanDecisions(new ArrayList<>(humanDecisions))
                .metadata(new HashMap<>(metadata))
                .awaitingHuman(awaitingHuman)
                .humanTaskId(humanTaskId)
                .build();
    }

    /**
     * Restore from checkpoint data.
     */
    public void restoreFromCheckpoint(CheckpointData checkpoint) {
        nodeResults.putAll(checkpoint.getNodeResults());
        nodeStates.putAll(checkpoint.getNodeStates());
        dataFlow.putAll(checkpoint.getDataFlow());
        errorHistory.addAll(checkpoint.getErrorHistory());
        humanDecisions.addAll(checkpoint.getHumanDecisions());
        metadata.putAll(checkpoint.getMetadata());
        awaitingHuman = checkpoint.isAwaitingHuman();
        humanTaskId = checkpoint.getHumanTaskId();
    }

    /**
     * Get all completed node IDs.
     */
    public Set<String> getCompletedNodes() {
        return nodeStates.entrySet().stream()
                .filter(e -> e.getValue() == NodeState.COMPLETED)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Get all failed node IDs.
     */
    public Set<String> getFailedNodes() {
        return nodeStates.entrySet().stream()
                .filter(e -> e.getValue() == NodeState.FAILED)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * Check if all nodes are completed.
     */
    public boolean isComplete() {
        int totalNodes = workflow.getNodes().size();
        long completedCount = nodeStates.values().stream()
                .filter(state -> state == NodeState.COMPLETED)
                .count();
        return completedCount == totalNodes;
    }

    /**
     * Check if workflow has any failures.
     */
    public boolean hasFailures() {
        return nodeStates.values().stream()
                .anyMatch(state -> state == NodeState.FAILED);
    }

    /**
     * Get error count.
     */
    public int getErrorCount() {
        return errorHistory.size();
    }

    /**
     * Get latest error.
     */
    public Optional<ErrorPayload> getLatestError() {
        if (errorHistory.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(errorHistory.get(errorHistory.size() - 1));
    }
}
