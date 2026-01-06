package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Complete execution history of a workflow run.
 * Contains all events, state changes, and execution records.
 * Used for debugging, auditing, and replay capabilities.
 */
@Data
@Builder(toBuilder = true)
public class ExecutionHistory {

    private final WorkflowRunId runId;
    private final WorkflowId workflowId;
    private final String workflowVersion;
    private final String tenantId;
    private final Instant created;
    private final Instant lastUpdated;

    // Timeline of events
    private final List<ExecutionEvent> events;

    // Node execution records
    private final List<NodeExecutionRecord> nodeExecutions;

    // State transitions
    private final List<StateTransition> stateTransitions;

    // Input/output snapshots
    private final Map<Instant, Map<String, Object>> inputSnapshots;
    private final Map<Instant, Map<String, Object>> outputSnapshots;

    // Metadata and statistics
    private final ExecutionStatistics statistics;
    private final Map<String, Object> metadata;

    @JsonCreator
    public ExecutionHistory(
            @JsonProperty("runId") WorkflowRunId runId,
            @JsonProperty("workflowId") WorkflowId workflowId,
            @JsonProperty("workflowVersion") String workflowVersion,
            @JsonProperty("tenantId") String tenantId,
            @JsonProperty("created") Instant created,
            @JsonProperty("lastUpdated") Instant lastUpdated,
            @JsonProperty("events") List<ExecutionEvent> events,
            @JsonProperty("nodeExecutions") List<NodeExecutionRecord> nodeExecutions,
            @JsonProperty("stateTransitions") List<StateTransition> stateTransitions,
            @JsonProperty("inputSnapshots") Map<Instant, Map<String, Object>> inputSnapshots,
            @JsonProperty("outputSnapshots") Map<Instant, Map<String, Object>> outputSnapshots,
            @JsonProperty("statistics") ExecutionStatistics statistics,
            @JsonProperty("metadata") Map<String, Object> metadata) {

        this.runId = runId;
        this.workflowId = workflowId;
        this.workflowVersion = workflowVersion;
        this.tenantId = tenantId;
        this.created = created != null ? created : Instant.now();
        this.lastUpdated = lastUpdated != null ? lastUpdated : Instant.now();
        this.events = events != null ? Collections.unmodifiableList(events) : List.of();
        this.nodeExecutions = nodeExecutions != null ? Collections.unmodifiableList(nodeExecutions) : List.of();
        this.stateTransitions = stateTransitions != null ? Collections.unmodifiableList(stateTransitions) : List.of();
        this.inputSnapshots = inputSnapshots != null ? Collections.unmodifiableMap(inputSnapshots) : Map.of();
        this.outputSnapshots = outputSnapshots != null ? Collections.unmodifiableMap(outputSnapshots) : Map.of();
        this.statistics = statistics != null ? statistics : ExecutionStatistics.empty();
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Map.of();
    }

    // Factory methods
    public static ExecutionHistory empty(WorkflowRunId runId, WorkflowId workflowId, String tenantId) {
        return ExecutionHistory.builder()
                .runId(runId)
                .workflowId(workflowId)
                .tenantId(tenantId)
                .created(Instant.now())
                .lastUpdated(Instant.now())
                .events(new ArrayList<>())
                .nodeExecutions(new ArrayList<>())
                .stateTransitions(new ArrayList<>())
                .inputSnapshots(new LinkedHashMap<>())
                .outputSnapshots(new LinkedHashMap<>())
                .statistics(ExecutionStatistics.empty())
                .metadata(Map.of("initialized", true))
                .build();
    }

    public static ExecutionHistory fromSnapshot(
            WorkflowRunSnapshot snapshot,
            List<ExecutionEvent> events,
            List<NodeExecutionRecord> nodeExecutions) {

        return ExecutionHistory.builder()
                .runId(snapshot.getRunId())
                .workflowId(WorkflowId.of(snapshot.getMetadata().getOrDefault("workflowId", "unknown").toString()))
                .workflowVersion(snapshot.getMetadata().getOrDefault("workflowVersion", "1.0.0").toString())
                .tenantId(snapshot.getContext().getTenantId())
                .created(snapshot.getSnapshotTime())
                .lastUpdated(Instant.now())
                .events(events)
                .nodeExecutions(nodeExecutions)
                .stateTransitions(List.of(
                        StateTransition.builder()
                                .fromState(null)
                                .toState(snapshot.getState())
                                .timestamp(snapshot.getSnapshotTime())
                                .reason("snapshot_restore")
                                .build()))
                .inputSnapshots(Map.of(
                        snapshot.getSnapshotTime(),
                        snapshot.getContext().getVariables()))
                .outputSnapshots(Map.of())
                .statistics(ExecutionStatistics.fromSnapshot(snapshot))
                .metadata(Map.of(
                        "source", "snapshot",
                        "snapshotId", snapshot.getSnapshotId(),
                        "checkpoint", snapshot.isCheckpoint()))
                .build();
    }

    // Utility methods
    public ExecutionHistory addEvent(ExecutionEvent event) {
        List<ExecutionEvent> newEvents = new ArrayList<>(this.events);
        newEvents.add(event);

        return this.toBuilder()
                .events(newEvents)
                .lastUpdated(Instant.now())
                .build();
    }

    public ExecutionHistory addNodeExecution(NodeExecutionRecord record) {
        List<NodeExecutionRecord> newExecutions = new ArrayList<>(this.nodeExecutions);
        newExecutions.add(record);

        return this.toBuilder()
                .nodeExecutions(newExecutions)
                .lastUpdated(Instant.now())
                .build();
    }

    public ExecutionHistory addStateTransition(StateTransition transition) {
        List<StateTransition> newTransitions = new ArrayList<>(this.stateTransitions);
        newTransitions.add(transition);

        return this.toBuilder()
                .stateTransitions(newTransitions)
                .lastUpdated(Instant.now())
                .build();
    }

    public ExecutionHistory recordInputSnapshot(Map<String, Object> inputs) {
        Map<Instant, Map<String, Object>> newSnapshots = new LinkedHashMap<>(this.inputSnapshots);
        newSnapshots.put(Instant.now(), Map.copyOf(inputs));

        return this.toBuilder()
                .inputSnapshots(newSnapshots)
                .lastUpdated(Instant.now())
                .build();
    }

    public ExecutionHistory recordOutputSnapshot(Map<String, Object> outputs) {
        Map<Instant, Map<String, Object>> newSnapshots = new LinkedHashMap<>(this.outputSnapshots);
        newSnapshots.put(Instant.now(), Map.copyOf(outputs));

        return this.toBuilder()
                .outputSnapshots(newSnapshots)
                .lastUpdated(Instant.now())
                .build();
    }

    public Optional<ExecutionEvent> getFirstEvent() {
        return events.stream().findFirst();
    }

    public Optional<ExecutionEvent> getLastEvent() {
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(events.get(events.size() - 1));
    }

    public Optional<NodeExecutionRecord> getNodeExecution(String nodeId) {
        return nodeExecutions.stream()
                .filter(record -> record.getNodeId().equals(nodeId))
                .findFirst();
    }

    public List<NodeExecutionRecord> getNodeExecutions(String nodeId) {
        return nodeExecutions.stream()
                .filter(record -> record.getNodeId().equals(nodeId))
                .toList();
    }

    public Optional<StateTransition> getLastStateTransition() {
        if (stateTransitions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(stateTransitions.get(stateTransitions.size() - 1));
    }

    public Optional<WorkflowRunState> getCurrentState() {
        return getLastStateTransition()
                .map(StateTransition::getToState);
    }

    public boolean hasErrors() {
        return nodeExecutions.stream()
                .anyMatch(record -> record.getStatus() == NodeExecutionStatus.FAILED) ||
                events.stream()
                        .anyMatch(event -> event.getEventType() == ExecutionEvent.ExecutionEventType.ERROR_OCCURRED ||
                                event.getEventType() == ExecutionEvent.ExecutionEventType.RUN_FAILED ||
                                event.getEventType() == ExecutionEvent.ExecutionEventType.NODE_FAILED);
    }

    public List<ExecutionError> getAllErrors() {
        List<ExecutionError> errors = new ArrayList<>();

        // Errors from node executions
        nodeExecutions.stream()
                .filter(record -> record.getError() != null)
                .map(NodeExecutionRecord::getError)
                .forEach(errors::add);

        // Errors from events
        events.stream()
                .filter(event -> event.getError() != null)
                .map(ExecutionEvent::getError)
                .forEach(errors::add);

        return Collections.unmodifiableList(errors);
    }

    public Duration getTotalDuration() {
        if (events.isEmpty()) {
            return Duration.ZERO;
        }

        Instant firstEvent = events.get(0).getTimestamp();
        Instant lastEvent = events.get(events.size() - 1).getTimestamp();

        return Duration.between(firstEvent, lastEvent);
    }

    public boolean isComplete() {
        return getCurrentState()
                .map(state -> state.isTerminal())
                .orElse(false);
    }

    // Nested classes
    @Data
    @Builder
    public static class ExecutionEvent {
        private final String eventId;
        private final ExecutionEventType eventType;
        private final Instant timestamp;
        private final String source;
        private final Map<String, Object> payload;
        private final ExecutionError error;
        private final Map<String, Object> metadata;

        public enum ExecutionEventType {
            RUN_CREATED,
            RUN_STARTED,
            RUN_COMPLETED,
            RUN_FAILED,
            RUN_CANCELLED,
            RUN_WAITING,
            RUN_RESUMED,
            NODE_STARTED,
            NODE_COMPLETED,
            NODE_FAILED,
            NODE_WAITING,
            STATE_UPDATED,
            SIGNAL_RECEIVED,
            ERROR_OCCURRED,
            COMPENSATION_STARTED,
            COMPENSATION_COMPLETED,
            RETRY_SCHEDULED,
            TIMER_EXPIRED,
            EXTERNAL_CALLBACK_RECEIVED,
            HUMAN_INTERVENTION_REQUIRED,
            HUMAN_INTERVENTION_COMPLETED
        }
    }

    @Data
    @Builder
    public static class StateTransition {
        private final WorkflowRunState fromState;
        private final WorkflowRunState toState;
        private final Instant timestamp;
        private final String reason;
        private final String initiatedBy;
        private final Map<String, Object> metadata;
    }

    @Data
    @Builder(toBuilder = true)
    public static class ExecutionStatistics {
        @Builder.Default
        private final int totalEvents = 0;

        @Builder.Default
        private final int totalNodeExecutions = 0;

        @Builder.Default
        private final int completedNodes = 0;

        @Builder.Default
        private final int failedNodes = 0;

        @Builder.Default
        private final int waitingNodes = 0;

        @Builder.Default
        private final int retriedNodes = 0;

        @Builder.Default
        private final Duration totalExecutionTime = Duration.ZERO;

        @Builder.Default
        private final Duration averageNodeExecutionTime = Duration.ZERO;

        @Builder.Default
        private final Map<String, Integer> nodeTypeCounts = Map.of();

        @Builder.Default
        private final Map<String, Duration> nodeTypeDurations = Map.of();

        @Builder.Default
        private final Map<String, Object> metrics = Map.of();

        public static ExecutionStatistics empty() {
            return ExecutionStatistics.builder().build();
        }

        public static ExecutionStatistics fromSnapshot(WorkflowRunSnapshot snapshot) {
            List<NodeExecutionRecord> executions = snapshot.getNodeHistory();

            int completed = (int) executions.stream()
                    .filter(e -> e.getStatus() == NodeExecutionStatus.COMPLETED)
                    .count();

            int failed = (int) executions.stream()
                    .filter(e -> e.getStatus() == NodeExecutionStatus.FAILED)
                    .count();

            Duration totalTime = executions.stream()
                    .map(e -> e.getDuration() != null ? e.getDuration() : Duration.ZERO)
                    .reduce(Duration.ZERO, Duration::plus);

            Duration avgTime = executions.isEmpty() ? Duration.ZERO : totalTime.dividedBy(executions.size());

            return ExecutionStatistics.builder()
                    .totalNodeExecutions(executions.size())
                    .completedNodes(completed)
                    .failedNodes(failed)
                    .totalExecutionTime(totalTime)
                    .averageNodeExecutionTime(avgTime)
                    .build();
        }

        public ExecutionStatistics merge(NodeExecutionRecord record) {
            Map<String, Integer> newNodeTypeCounts = new HashMap<>(nodeTypeCounts);
            Map<String, Duration> newNodeTypeDurations = new HashMap<>(nodeTypeDurations);

            // Extract node type from metadata
            String nodeType = record.getMetadata() != null
                    ? (String) record.getMetadata().getOrDefault("nodeType", "unknown")
                    : "unknown";

            // Update counts
            newNodeTypeCounts.put(nodeType, newNodeTypeCounts.getOrDefault(nodeType, 0) + 1);

            // Update durations
            Duration currentDuration = newNodeTypeDurations.getOrDefault(nodeType, Duration.ZERO);
            Duration recordDuration = record.getDuration() != null ? record.getDuration() : Duration.ZERO;
            newNodeTypeDurations.put(nodeType, currentDuration.plus(recordDuration));

            // Update statistics
            int newTotalNodeExecutions = totalNodeExecutions + 1;
            int newCompletedNodes = completedNodes +
                    (record.getStatus() == NodeExecutionStatus.COMPLETED ? 1 : 0);
            int newFailedNodes = failedNodes +
                    (record.getStatus() == NodeExecutionStatus.FAILED ? 1 : 0);
            int newWaitingNodes = waitingNodes +
                    (record.getStatus() == NodeExecutionStatus.WAITING ? 1 : 0);
            int newRetriedNodes = retriedNodes +
                    (record.getAttempt() > 1 ? 1 : 0);

            Duration newTotalExecutionTime = totalExecutionTime.plus(
                    record.getDuration() != null ? record.getDuration() : Duration.ZERO);

            Duration newAverageNodeExecutionTime = newTotalNodeExecutions > 0
                    ? newTotalExecutionTime.dividedBy(newTotalNodeExecutions)
                    : Duration.ZERO;

            return this.toBuilder()
                    .totalNodeExecutions(newTotalNodeExecutions)
                    .completedNodes(newCompletedNodes)
                    .failedNodes(newFailedNodes)
                    .waitingNodes(newWaitingNodes)
                    .retriedNodes(newRetriedNodes)
                    .totalExecutionTime(newTotalExecutionTime)
                    .averageNodeExecutionTime(newAverageNodeExecutionTime)
                    .nodeTypeCounts(Collections.unmodifiableMap(newNodeTypeCounts))
                    .nodeTypeDurations(Collections.unmodifiableMap(newNodeTypeDurations))
                    .build();
        }

        public ExecutionStatistics merge(ExecutionEvent event) {
            int newTotalEvents = totalEvents + 1;

            Map<String, Object> newMetrics = new HashMap<>(metrics);
            newMetrics.put("lastEventType", event.getEventType().name());
            newMetrics.put("lastEventTime", event.getTimestamp().toString());

            return this.toBuilder()
                    .totalEvents(newTotalEvents)
                    .metrics(Collections.unmodifiableMap(newMetrics))
                    .build();
        }

        public double getSuccessRate() {
            if (totalNodeExecutions == 0) {
                return 1.0;
            }
            return (double) completedNodes / totalNodeExecutions;
        }

        public double getFailureRate() {
            if (totalNodeExecutions == 0) {
                return 0.0;
            }
            return (double) failedNodes / totalNodeExecutions;
        }

        public double getRetryRate() {
            if (totalNodeExecutions == 0) {
                return 0.0;
            }
            return (double) retriedNodes / totalNodeExecutions;
        }
    }

    // Convenience methods for common queries
    public List<ExecutionEvent> getEventsByType(ExecutionEvent.ExecutionEventType type) {
        return events.stream()
                .filter(event -> event.getEventType() == type)
                .toList();
    }

    public List<NodeExecutionRecord> getExecutionsByStatus(NodeExecutionStatus status) {
        return nodeExecutions.stream()
                .filter(record -> record.getStatus() == status)
                .toList();
    }

    public Optional<Instant> getStartTime() {
        return events.stream()
                .filter(event -> event.getEventType() == ExecutionEvent.ExecutionEventType.RUN_STARTED)
                .map(ExecutionEvent::getTimestamp)
                .findFirst();
    }

    public Optional<Instant> getEndTime() {
        return events.stream()
                .filter(event -> event.getEventType() == ExecutionEvent.ExecutionEventType.RUN_COMPLETED ||
                        event.getEventType() == ExecutionEvent.ExecutionEventType.RUN_FAILED ||
                        event.getEventType() == ExecutionEvent.ExecutionEventType.RUN_CANCELLED)
                .map(ExecutionEvent::getTimestamp)
                .findFirst();
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", runId.getId());
        summary.put("workflowId", workflowId.getId());
        summary.put("workflowVersion", workflowVersion);
        summary.put("tenantId", tenantId);
        summary.put("status", getCurrentState().map(Enum::name).orElse("UNKNOWN"));
        summary.put("totalEvents", events.size());
        summary.put("totalNodeExecutions", nodeExecutions.size());
        summary.put("completedNodes", statistics.getCompletedNodes());
        summary.put("failedNodes", statistics.getFailedNodes());
        summary.put("successRate", String.format("%.2f%%", statistics.getSuccessRate() * 100));
        summary.put("totalDuration", getTotalDuration().toString());
        summary.put("startTime", getStartTime().map(Instant::toString).orElse("N/A"));
        summary.put("endTime", getEndTime().map(Instant::toString).orElse("N/A"));
        summary.put("hasErrors", hasErrors());
        summary.put("isComplete", isComplete());
        return Collections.unmodifiableMap(summary);
    }
}