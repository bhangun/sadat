package tech.kayys.silat.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import tech.kayys.silat.model.WorkflowId;
import tech.kayys.silat.model.WorkflowRunId;
import tech.kayys.silat.model.WorkflowRunSnapshot;
import tech.kayys.silat.model.WorkflowRunState;

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
    private final List<ExecutionEventHistory> events;

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
            @JsonProperty("events") List<ExecutionEventHistory> events,
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

    public List<ExecutionEventHistory> getEventsByType(String eventType) {
        return events.stream()
                .filter(e -> e.getEventType().name().equals(eventType))
                .toList();
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

    /**
     * Create ExecutionHistory from a list of domain events
     * Converts tech.kayys.silat.model.event.ExecutionEvent to
     * ExecutionHistory.ExecutionEvent
     */
    public static ExecutionHistory fromEvents(
            WorkflowRunId runId,
            List<tech.kayys.silat.model.event.ExecutionEvent> domainEvents) {

        // Convert domain events to history events
        List<ExecutionEventHistory> historyEvents = domainEvents.stream()
                .map(domainEvent -> ExecutionEventHistory.builder()
                        .eventId(domainEvent.eventId())
                        .eventType(mapEventType(domainEvent.eventType()))
                        .timestamp(domainEvent.occurredAt())
                        .source("event-store")
                        .payload(Map.of())
                        .metadata(Map.of("domainEventType", domainEvent.eventType()))
                        .build())
                .toList();

        return ExecutionHistory.builder()
                .runId(runId)
                .workflowId(WorkflowId.of("unknown")) // Will be enriched later
                .workflowVersion("unknown")
                .tenantId("unknown")
                .created(historyEvents.isEmpty() ? Instant.now() : historyEvents.get(0).getTimestamp())
                .lastUpdated(Instant.now())
                .events(historyEvents)
                .nodeExecutions(new ArrayList<>())
                .stateTransitions(new ArrayList<>())
                .inputSnapshots(new LinkedHashMap<>())
                .outputSnapshots(new LinkedHashMap<>())
                .statistics(ExecutionStatistics.empty())
                .metadata(Map.of("source", "domain-events"))
                .build();
    }

    /**
     * Map domain event type string to ExecutionEventType enum
     */
    private static ExecutionEventHistory.ExecutionEventType mapEventType(String eventType) {
        return switch (eventType) {
            case "WorkflowStartedEvent" -> ExecutionEventHistory.ExecutionEventType.RUN_STARTED;
            case "NodeScheduledEvent" -> ExecutionEventHistory.ExecutionEventType.NODE_STARTED;
            case "NodeStartedEvent" -> ExecutionEventHistory.ExecutionEventType.NODE_STARTED;
            case "NodeCompletedEvent" -> ExecutionEventHistory.ExecutionEventType.NODE_COMPLETED;
            case "NodeFailedEvent" -> ExecutionEventHistory.ExecutionEventType.NODE_FAILED;
            case "WorkflowSuspendedEvent" -> ExecutionEventHistory.ExecutionEventType.RUN_WAITING;
            case "WorkflowResumedEvent" -> ExecutionEventHistory.ExecutionEventType.RUN_RESUMED;
            case "WorkflowCompletedEvent" -> ExecutionEventHistory.ExecutionEventType.RUN_COMPLETED;
            case "WorkflowFailedEvent" -> ExecutionEventHistory.ExecutionEventType.RUN_FAILED;
            case "WorkflowCancelledEvent" -> ExecutionEventHistory.ExecutionEventType.RUN_CANCELLED;
            default -> ExecutionEventHistory.ExecutionEventType.STATE_UPDATED;
        };
    }

    public static ExecutionHistory fromSnapshot(
            WorkflowRunSnapshot snapshot,
            List<ExecutionEventHistory> events,
            List<NodeExecutionRecord> nodeExecutions) {

        return ExecutionHistory.builder()
                .runId(snapshot.id())
                .workflowId(WorkflowId.of(snapshot.definitionId().value()))
                .workflowVersion("1.0.0")
                .tenantId(snapshot.tenantId().value())
                .created(snapshot.createdAt())
                .lastUpdated(Instant.now())
                .events(events)
                .nodeExecutions(nodeExecutions)
                .stateTransitions(List.of()) // Simplified since some info is missing
                .inputSnapshots(Map.of(
                        snapshot.createdAt(),
                        snapshot.variables()))
                .outputSnapshots(Map.of())
                .statistics(ExecutionStatistics.builder().build()) // Simplified
                .metadata(Map.of("source", "snapshot"))
                .build();
    }

    // Utility methods
    public ExecutionHistory addEvent(ExecutionEventHistory event) {
        List<ExecutionEventHistory> newEvents = new ArrayList<>(this.events);
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

    public Optional<ExecutionEventHistory> getFirstEvent() {
        return events.stream().findFirst();
    }

    public Optional<ExecutionEventHistory> getLastEvent() {
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
                        .anyMatch(
                                event -> event.getEventType() == ExecutionEventHistory.ExecutionEventType.ERROR_OCCURRED
                                        ||
                                        event.getEventType() == ExecutionEventHistory.ExecutionEventType.RUN_FAILED ||
                                        event.getEventType() == ExecutionEventHistory.ExecutionEventType.NODE_FAILED);
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
                .map(ExecutionEventHistory::getError)
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
    public static class ExecutionEventHistory {
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
            // Updated to be compatible with new WorkflowRunSnapshot
            return ExecutionStatistics.builder().build();
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

        public ExecutionStatistics merge(ExecutionEventHistory event) {
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
    public List<ExecutionEventHistory> getEventsByType(ExecutionEventHistory.ExecutionEventType type) {
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
                .filter(event -> event.getEventType() == ExecutionEventHistory.ExecutionEventType.RUN_STARTED)
                .map(ExecutionEventHistory::getTimestamp)
                .findFirst();
    }

    public Optional<Instant> getEndTime() {
        return events.stream()
                .filter(event -> event.getEventType() == ExecutionEventHistory.ExecutionEventType.RUN_COMPLETED ||
                        event.getEventType() == ExecutionEventHistory.ExecutionEventType.RUN_FAILED ||
                        event.getEventType() == ExecutionEventHistory.ExecutionEventType.RUN_CANCELLED)
                .map(ExecutionEventHistory::getTimestamp)
                .findFirst();
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", runId.value());
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