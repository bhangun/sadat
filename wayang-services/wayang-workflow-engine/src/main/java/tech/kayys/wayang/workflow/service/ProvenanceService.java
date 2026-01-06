package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.api.dto.AuditPayload;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.domain.AuditEvent;
import tech.kayys.wayang.workflow.domain.WorkflowRun;

import tech.kayys.wayang.workflow.model.ActorType;
import tech.kayys.wayang.workflow.model.AuditConfiguration;
import tech.kayys.wayang.workflow.model.ErrorDecision;
import tech.kayys.wayang.workflow.model.EventType;
import tech.kayys.wayang.workflow.model.ExecutionContext;
import tech.kayys.wayang.workflow.repository.AuditEventRepository;

import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.*;
import java.security.MessageDigest;

/**
 * ProvenanceService - Comprehensive audit and provenance tracking.
 * 
 * Responsibilities:
 * - Record all workflow and node execution events
 * - Maintain tamper-proof audit trail with cryptographic hashing
 * - Support event replay for debugging
 * - Provide queryable event history
 * - Generate compliance reports
 * 
 * Design Principles:
 * - Append-only event store (no updates/deletes)
 * - Cryptographic chain of custody
 * - Fast async writes (fire-and-forget for non-critical)
 * - Efficient querying with indexes
 * - GDPR-compliant data retention
 */
@ApplicationScoped
public class ProvenanceService {

    private static final Logger LOG = Logger.getLogger(ProvenanceService.class);

    @Inject
    AuditEventRepository eventRepository;

    @Inject
    AuditConfiguration auditConfig;

    @Inject
    CryptoService cryptoService;

    // Previous hash for chaining (per run)
    private final Map<String, String> previousHashes = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Log workflow start event.
     */
    public Uni<Void> logWorkflowStart(WorkflowRun run, WorkflowDefinition workflow) {
        return createEvent(
                EventType.WORKFLOW_STARTED,
                run.getRunId(),
                null,
                Map.of(
                        "workflow_id", workflow.getId(),
                        "workflow_version", workflow.getVersion(),
                        "tenant_id", run.getTenantId(),
                        "inputs", run.getInputs()),
                ActorType.SYSTEM,
                "workflow-engine").replaceWithVoid();
    }

    /**
     * Log workflow completion.
     */
    public Uni<Void> logWorkflowComplete(WorkflowRun run) {
        long durationMs = java.time.Duration.between(
                run.getStartedAt(),
                run.getCompletedAt()).toMillis();

        return createEvent(
                EventType.WORKFLOW_COMPLETED,
                run.getRunId(),
                null,
                Map.of(
                        "duration_ms", durationMs,
                        "status", run.getStatus(),
                        "outputs", run.getOutputs()),
                ActorType.SYSTEM,
                "workflow-engine").replaceWithVoid();
    }

    /**
     * Log workflow failure.
     */
    public Uni<Void> logRunFailed(WorkflowRun run, String error) {
        return createEvent(
                EventType.WORKFLOW_FAILED,
                run.getRunId(),
                null,
                Map.of(
                        "error", error,
                        "status", run.getStatus()),
                ActorType.SYSTEM,
                "workflow-engine").replaceWithVoid();
    }

    /**
     * Log workflow cancellation.
     */
    public Uni<Void> logRunCancelled(WorkflowRun run, String reason) {
        return createEvent(
                EventType.WORKFLOW_CANCELLED,
                run.getRunId(),
                null,
                Map.of(
                        "reason", reason,
                        "cancelled_at", Instant.now()),
                ActorType.SYSTEM,
                "workflow-engine").replaceWithVoid();
    }

    /**
     * Log workflow resume.
     */
    public Uni<Void> logRunResumed(WorkflowRun run) {
        return createEvent(
                EventType.WORKFLOW_RESUMED,
                run.getRunId(),
                null,
                Map.of(
                        "previous_status", run.getStatus(),
                        "resumed_at", Instant.now()),
                ActorType.SYSTEM,
                "workflow-engine").replaceWithVoid();
    }

    /**
     * Log workflow created.
     */
    public Uni<Void> logRunCreated(WorkflowRun run) {
        return createEvent(
                EventType.WORKFLOW_CREATED,
                run.getRunId(),
                null,
                Map.of(
                        "workflow_id", run.getWorkflowId(),
                        "tenant_id", run.getTenantId(),
                        "initiated_by", run.getTriggeredBy()),
                ActorType.USER,
                run.getTriggeredBy()).replaceWithVoid();
    }

    /**
     * Log status change.
     */
    public Uni<Void> logStatusChange(String runId, RunStatus from, RunStatus to) {
        return createEvent(
                EventType.STATUS_CHANGED,
                runId,
                null,
                Map.of(
                        "from_status", from,
                        "to_status", to,
                        "changed_at", Instant.now()),
                ActorType.SYSTEM,
                "workflow-engine").replaceWithVoid();
    }

    /**
     * Log node execution start.
     */
    public Uni<Void> logNodeExecution(
            NodeContext context,
            NodeDefinition nodeDef,
            NodeExecutionResult result) {

        EventType eventType = result.isSuccess()
                ? EventType.NODE_SUCCEEDED
                : EventType.NODE_FAILED;

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("node_type", nodeDef.getType());
        eventData.put("status", result.getStatus());

        if (result.isSuccess()) {
            eventData.put("outputs", redactSensitiveData(result.getOutput()));
        } else if (result.isError()) {
            eventData.put("error", result.getError());
        }

        return createEvent(
                eventType,
                context.getRunId(),
                context.getNodeId(),
                eventData,
                ActorType.SYSTEM,
                "node-executor").replaceWithVoid();
    }

    /**
     * Log node success.
     */
    public Uni<Void> logNodeSuccess(NodeContext context, NodeExecutionResult result) {
        return createEvent(
                EventType.NODE_SUCCEEDED,
                context.getRunId(),
                context.getNodeId(),
                Map.of(
                        "outputs", redactSensitiveData(result.getOutput())),
                ActorType.SYSTEM,
                "node-executor").replaceWithVoid();
    }

    /**
     * Log node error.
     */
    public Uni<Void> logNodeError(String runId, String nodeId, ErrorPayload error) {
        return createEvent(
                EventType.NODE_FAILED,
                runId,
                nodeId,
                Map.of(
                        "error_type", error.getType(),
                        "error_message", error.getMessage(),
                        "retryable", error.isRetryable(),
                        "attempt", error.getAttempt()),
                ActorType.SYSTEM,
                "node-executor").replaceWithVoid();
    }

    /**
     * Log node blocked by guardrails.
     */
    public Uni<Void> logNodeBlocked(NodeContext context, String reason) {
        return createEvent(
                EventType.NODE_BLOCKED,
                context.getRunId(),
                context.getNodeId(),
                Map.of(
                        "reason", reason,
                        "blocked_at", Instant.now()),
                ActorType.SYSTEM,
                "guardrails-engine").replaceWithVoid();
    }

    /**
     * Log error occurrence.
     */
    public Uni<Void> logError(
            ErrorPayload error,
            NodeDefinition nodeDef,
            ExecutionContext context) {

        return createEvent(
                EventType.ERROR_OCCURRED,
                context.getExecutionId(),
                error.getOriginNode(),
                Map.of(
                        "error_type", error.getType(),
                        "error_message", error.getMessage(),
                        "retryable", error.isRetryable(),
                        "details", error.getDetails()),
                ActorType.SYSTEM,
                "error-orchestrator").replaceWithVoid();
    }

    /**
     * Log error handling decision.
     */
    public Uni<Void> logErrorDecision(
            ErrorPayload error,
            ErrorDecision decision,
            ExecutionContext context) {

        return createEvent(
                EventType.ERROR_DECISION,
                context.getExecutionId(),
                error.getOriginNode(),
                Map.of(
                        "action", decision.getAction(),
                        "reason", decision.getReason(),
                        "metadata", decision.getMetadata()),
                ActorType.SYSTEM,
                "error-orchestrator").replaceWithVoid();
    }

    /**
     * Log self-healing attempt.
     */
    public Uni<Void> logSelfHealing(
            ErrorPayload error,
            HealedContext healed,
            ExecutionContext context) {

        return createEvent(
                EventType.SELF_HEAL_ATTEMPT,
                context.getExecutionId(),
                error.getOriginNode(),
                Map.of(
                        "healed", healed.isHealed(),
                        "repair_log", healed.getRepairLog(),
                        "failure_reason", healed.getFailureReason()),
                ActorType.SYSTEM,
                "self-healing-service").replaceWithVoid();
    }

    /**
     * Log human task creation.
     */
    public Uni<Void> logHumanTaskCreated(HTILTask task) {
        return createEvent(
                EventType.HUMAN_TASK_CREATED,
                task.getRunId(),
                task.getNodeId(),
                Map.of(
                        "task_id", task.getId(),
                        "assigned_to", task.getAssignedTo(),
                        "ttl_minutes", task.getTtlMinutes()),
                ActorType.SYSTEM,
                "hitl-service").replaceWithVoid();
    }

    /**
     * Log human task completion.
     */
    public Uni<Void> logHumanTaskCompleted(
            HTILTask task,
            HTILTaskResult result,
            String operatorId) {

        return createEvent(
                EventType.HUMAN_TASK_COMPLETED,
                task.getRunId(),
                task.getNodeId(),
                Map.of(
                        "task_id", task.getId(),
                        "action", result.getAction(),
                        "operator_notes", result.getNotes()),
                ActorType.HUMAN,
                operatorId).replaceWithVoid();
    }

    /**
     * Log policy evaluation.
     */
    public Uni<Void> logPolicyEvaluation(
            String runId,
            String nodeId,
            String policyName,
            boolean allowed,
            String reason) {

        return createEvent(
                EventType.POLICY_EVALUATED,
                runId,
                nodeId,
                Map.of(
                        "policy_name", policyName,
                        "allowed", allowed,
                        "reason", reason),
                ActorType.SYSTEM,
                "policy-engine").replaceWithVoid();
    }

    /**
     * Log guardrail evaluation.
     */
    public Uni<Void> logGuardrailEvaluation(
            String runId,
            String nodeId,
            String guardrailName,
            boolean passed,
            String reason) {

        return createEvent(
                EventType.GUARDRAIL_CHECKED,
                runId,
                nodeId,
                Map.of(
                        "guardrail_name", guardrailName,
                        "passed", passed,
                        "reason", reason),
                ActorType.SYSTEM,
                "guardrails-engine").replaceWithVoid();
    }

    /**
     * Get all events for a workflow run.
     */
    public Uni<List<AuditEvent>> getRunEvents(String runId) {
        return eventRepository.findByRunId(runId);
    }

    /**
     * Get events for a specific node.
     */
    public Uni<List<AuditEvent>> getNodeEvents(String runId, String nodeId) {
        return eventRepository.findByRunIdAndNodeId(runId, nodeId);
    }

    /**
     * Get events by type.
     */
    public Uni<List<AuditEvent>> getEventsByType(
            String runId,
            EventType eventType) {
        return eventRepository.findByRunIdAndType(runId, eventType);
    }

    /**
     * Get event timeline for visualization.
     */
    public Uni<EventTimeline> getTimeline(String runId) {
        return getRunEvents(runId)
                .map(events -> {
                    List<TimelineEntry> entries = events.stream()
                            .map(event -> TimelineEntry.builder()
                                    .timestamp(event.getTimestamp())
                                    .eventType(event.getEventType())
                                    .nodeId(event.getNodeId())
                                    .summary(event.getSummary())
                                    .build())
                            .sorted(Comparator.comparing(TimelineEntry::getTimestamp))
                            .toList();

                    return EventTimeline.builder()
                            .runId(runId)
                            .entries(entries)
                            .build();
                });
    }

    /**
     * Verify event chain integrity.
     */
    public Uni<ChainVerification> verifyChain(String runId) {
        return getRunEvents(runId)
                .map(events -> {
                    List<String> violations = new ArrayList<>();
                    String previousHash = null;

                    for (AuditEvent event : events) {
                        // Verify hash
                        String computedHash = computeHash(event, previousHash);
                        if (!computedHash.equals(event.getHash())) {
                            violations.add("Hash mismatch at event " + event.getId());
                        }
                        previousHash = event.getHash();
                    }

                    return ChainVerification.builder()
                            .runId(runId)
                            .valid(violations.isEmpty())
                            .violations(violations)
                            .totalEvents(events.size())
                            .build();
                });
    }

    /**
     * Generate compliance report.
     */
    public Uni<ComplianceReport> generateReport(
            String runId,
            ReportType reportType) {

        return getRunEvents(runId)
                .map(events -> {
                    ComplianceReport report = new ComplianceReport();
                    report.setRunId(runId);
                    report.setReportType(reportType);
                    report.setGeneratedAt(Instant.now());

                    // Calculate statistics
                    report.setTotalEvents(events.size());
                    report.setNodeExecutions(
                            events.stream()
                                    .filter(e -> e.getEventType() == EventType.NODE_SUCCEEDED
                                            ||
                                            e.getEventType() == EventType.NODE_FAILED)
                                    .count());
                    report.setHumanInterventions(
                            events.stream()
                                    .filter(e -> e.getActorType() == ActorType.HUMAN)
                                    .count());
                    report.setErrorCount(
                            events.stream()
                                    .filter(e -> e.getEventType() == EventType.ERROR_OCCURRED)
                                    .count());

                    // Event breakdown
                    Map<EventType, Long> breakdown = events.stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    AuditEvent::getEventType,
                                    java.util.stream.Collectors.counting()));
                    report.setEventBreakdown(breakdown);

                    return report;
                });
    }

    /**
     * Cleanup old events based on retention policy.
     */
    public Uni<Long> cleanupOldEvents(int retentionDays) {
        Instant threshold = Instant.now().minusSeconds(retentionDays * 24 * 60 * 60L);
        return eventRepository.deleteOldEvents(threshold);
    }

    /**
     * Create and persist audit event with chaining.
     */
    private Uni<AuditEvent> createEvent(
            EventType eventType,
            String runId,
            String nodeId,
            Map<String, Object> eventData,
            ActorType actorType,
            String actorId) {

        // Skip if audit disabled
        if (!auditConfig.isEnabled()) {
            return Uni.createFrom().nullItem();
        }

        AuditEvent event = new AuditEvent();
        event.setId(UUID.randomUUID().toString());
        event.setRunId(runId);
        event.setNodeId(nodeId);
        event.setEventType(eventType);
        event.setEventData(eventData);
        event.setActorType(actorType);
        event.setActorId(actorId);
        event.setTimestamp(Instant.now());

        // Compute hash with previous hash (chaining)
        String previousHash = previousHashes.get(runId);
        if (auditConfig.isHashingEnabled()) {
            String hash = computeHash(event, previousHash);
            event.setHash(hash);
            previousHashes.put(runId, hash);
        }

        // Generate summary
        event.setSummary(generateSummary(event));

        // Persist asynchronously (fire-and-forget for performance)
        return eventRepository.persist(event)
                .onFailure()
                .invoke(th -> LOG.errorf(th, "Failed to persist audit event: %s", event.getId()))
                .onFailure().recoverWithNull(); // Don't fail workflow on audit errors
    }

    /**
     * Compute cryptographic hash for event chaining.
     */
    private String computeHash(AuditEvent event, String previousHash) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String data = String.format("%s|%s|%s|%s|%s|%s",
                    event.getRunId(),
                    event.getEventType(),
                    event.getTimestamp(),
                    event.getActorId(),
                    previousHash != null ? previousHash : "",
                    event.getEventData().toString());

            byte[] hash = digest.digest(data.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            LOG.error("Failed to compute hash", e);
            return "ERROR";
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * Generate human-readable summary.
     */
    private String generateSummary(AuditEvent event) {
        return switch (event.getEventType()) {
            case WORKFLOW_STARTED -> "Workflow started";
            case WORKFLOW_COMPLETED -> "Workflow completed successfully";
            case WORKFLOW_FAILED -> "Workflow failed: " + event.getEventData().get("error");
            case NODE_SUCCEEDED -> "Node " + event.getNodeId() + " succeeded";
            case NODE_FAILED -> "Node " + event.getNodeId() + " failed";
            case HUMAN_TASK_CREATED -> "Human review requested";
            case HUMAN_TASK_COMPLETED -> "Human decision recorded";
            case ERROR_OCCURRED -> "Error: " + event.getEventData().get("error_message");
            default -> event.getEventType().name();
        };
    }

    /**
     * Redact sensitive data based on policy.
     */
    private Map<String, Object> redactSensitiveData(Map<String, Object> data) {
        if (!auditConfig.isPiiRedactionEnabled()) {
            return data;
        }

        Map<String, Object> redacted = new HashMap<>(data);
        // Implement PII detection and redaction logic
        // This is simplified - real implementation would use patterns/ML
        for (String key : redacted.keySet()) {
            if (key.toLowerCase().contains("password") ||
                    key.toLowerCase().contains("secret") ||
                    key.toLowerCase().contains("token")) {
                redacted.put(key, "***REDACTED***");
            }
        }
        return redacted;
    }

    public void log(AuditPayload auditPayload) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'log'");
    }

    public static class NodeContext {
        private final String runId;
        private final String nodeId;

        public NodeContext(String runId, String nodeId) {
            this.runId = runId;
            this.nodeId = nodeId;
        }

        public String getRunId() {
            return runId;
        }

        public String getNodeId() {
            return nodeId;
        }
    }

    public static class NodeExecutionResult {
        private final boolean success;
        private final String status;
        private final Map<String, Object> output;
        private final String error;

        public NodeExecutionResult(boolean success, String status, Map<String, Object> output, String error) {
            this.success = success;
            this.status = status;
            this.output = output;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getStatus() {
            return status;
        }

        public Map<String, Object> getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public boolean isError() {
            return error != null;
        }

        public Map<String, Object> getOutputChannels() {
            return null;
        }
    }

    public static class HealedContext {
        private final boolean healed;
        private final String repairLog;
        private final String failureReason;

        public HealedContext(boolean healed, String repairLog, String failureReason) {
            this.healed = healed;
            this.repairLog = repairLog;
            this.failureReason = failureReason;
        }

        public boolean isHealed() {
            return healed;
        }

        public String getRepairLog() {
            return repairLog;
        }

        public String getFailureReason() {
            return failureReason;
        }
    }

    public static class HTILTask {
        private final String runId;
        private final String nodeId;
        private final String id;
        private final String assignedTo;
        private final int ttlMinutes;

        public HTILTask(String runId, String nodeId, String id, String assignedTo, int ttlMinutes) {
            this.runId = runId;
            this.nodeId = nodeId;
            this.id = id;
            this.assignedTo = assignedTo;
            this.ttlMinutes = ttlMinutes;
        }

        public String getRunId() {
            return runId;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getId() {
            return id;
        }

        public String getAssignedTo() {
            return assignedTo;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }
    }

    public static class HTILTaskResult {
        private final String action;
        private final String notes;

        public HTILTaskResult(String action, String notes) {
            this.action = action;
            this.notes = notes;
        }

        public String getAction() {
            return action;
        }

        public String getNotes() {
            return notes;
        }
    }

    @lombok.Builder
    public static class EventTimeline {
        private String runId;
        private List<TimelineEntry> entries;

        public String getRunId() {
            return runId;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public List<TimelineEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<TimelineEntry> entries) {
            this.entries = entries;
        }

    }

    @lombok.Builder
    @lombok.Getter
    public static class TimelineEntry {
        private final Instant timestamp;
        private final EventType eventType;
        private final String nodeId;
        private final String summary;

        public Instant getTimestamp() {
            return timestamp;
        }

    }

    @lombok.Builder
    @lombok.Getter
    public static class ChainVerification {
        private final String runId;
        private final boolean valid;
        private final List<String> violations;
        private final int totalEvents;

        public ChainVerification(String runId, boolean valid, List<String> violations, int totalEvents) {
            this.runId = runId;
            this.valid = valid;
            this.violations = violations;
            this.totalEvents = totalEvents;
        }

        public String getRunId() {
            return runId;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getViolations() {
            return violations;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public static ChainVerificationBuilder builder() {
            return new ChainVerificationBuilder();
        }

        public static class ChainVerificationBuilder {
            private String runId;
            private boolean valid;
            private List<String> violations;
            private int totalEvents;

            public ChainVerificationBuilder runId(String runId) {
                this.runId = runId;
                return this;
            }

            public ChainVerificationBuilder valid(boolean valid) {
                this.valid = valid;
                return this;
            }

            public ChainVerificationBuilder violations(List<String> violations) {
                this.violations = violations;
                return this;
            }

            public ChainVerificationBuilder totalEvents(int totalEvents) {
                this.totalEvents = totalEvents;
                return this;
            }

            public ChainVerification build() {
                return new ChainVerification(runId, valid, violations, totalEvents);
            }
        }
    }

    public static class ComplianceReport {
        private String runId;
        private ReportType reportType;
        private Instant generatedAt;
        private int totalEvents;
        private long nodeExecutions;
        private long humanInterventions;
        private long errorCount;
        private Map<EventType, Long> eventBreakdown;

        public String getRunId() {
            return runId;
        }

        public ReportType getReportType() {
            return reportType;
        }

        public Instant getGeneratedAt() {
            return generatedAt;
        }

        public int getTotalEvents() {
            return totalEvents;
        }

        public long getNodeExecutions() {
            return nodeExecutions;
        }

        public long getHumanInterventions() {
            return humanInterventions;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public Map<EventType, Long> getEventBreakdown() {
            return eventBreakdown;
        }

        public void setRunId(String runId) {
            this.runId = runId;
        }

        public void setReportType(ReportType reportType) {
            this.reportType = reportType;
        }

        public void setGeneratedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
        }

        public void setTotalEvents(int totalEvents) {
            this.totalEvents = totalEvents;
        }

        public void setNodeExecutions(long nodeExecutions) {
            this.nodeExecutions = nodeExecutions;
        }

        public void setHumanInterventions(long humanInterventions) {
            this.humanInterventions = humanInterventions;
        }

        public void setErrorCount(long errorCount) {
            this.errorCount = errorCount;
        }

        public void setEventBreakdown(Map<EventType, Long> eventBreakdown) {
            this.eventBreakdown = eventBreakdown;
        }
    }
}
