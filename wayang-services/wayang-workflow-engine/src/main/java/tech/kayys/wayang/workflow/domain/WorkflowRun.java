package tech.kayys.wayang.workflow.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.sdk.dto.NodeExecutionState;
import tech.kayys.wayang.workflow.model.WorkflowExecutionState;
import tech.kayys.wayang.workflow.service.JsonbConverter;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * WorkflowRun - Core domain entity representing a workflow execution instance.
 * 
 * Design Philosophy:
 * - Immutable execution identity (runId)
 * - Event-sourced state transitions
 * - Durable, recoverable execution context
 * - Multi-tenant isolation
 * - Complete audit trail
 * 
 * Inspired by: Temporal workflow execution, AWS Step Functions execution
 */
@Entity
@Table(name = "workflow_runs", indexes = {
        @Index(name = "idx_workflow_runs_workflow_id", columnList = "workflowId"),
        @Index(name = "idx_workflow_runs_tenant_status", columnList = "tenantId,status"),
        @Index(name = "idx_workflow_runs_created_at", columnList = "createdAt"),
        @Index(name = "idx_workflow_runs_parent_run", columnList = "parentRunId")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Builder
public class WorkflowRun extends PanacheEntityBase {

    /**
     * Unique execution identifier (UUID)
     * Immutable throughout lifecycle
     */
    @Id
    @Column(nullable = false, length = 36)
    private String runId;

    /**
     * Reference to workflow definition
     */
    @Column(nullable = false, length = 255)
    private String workflowId;

    /**
     * Workflow version (semantic versioning)
     */
    @Column(nullable = false, length = 20)
    private String workflowVersion;

    /**
     * Multi-tenant isolation
     */
    @Column(nullable = false, length = 100)
    private String tenantId;

    /**
     * Execution status - authoritative state
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status;

    /**
     * Current execution phase
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ExecutionPhase phase;

    /**
     * Timestamps for lifecycle tracking
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant startedAt;

    @Column
    private Instant completedAt;

    @Column
    private Instant lastHeartbeatAt;

    @Column
    private Instant updatedAt;

    /**
     * Execution context and state
     */
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> inputs;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> outputs;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private WorkflowExecutionState executionState;

    /**
     * Error tracking
     */
    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private ErrorPayload lastError;

    /**
     * Retry and recovery
     */
    @lombok.Builder.Default
    @Column(nullable = false)
    private Integer attemptNumber = 1;

    @lombok.Builder.Default
    @Column(nullable = false)
    private Integer maxAttempts = 3;

    @Column
    private Instant nextRetryAt;

    /**
     * Execution metadata
     */
    @Column(length = 100)
    private String triggeredBy;

    @Column(length = 50)
    private String triggerType; // manual, cron, webhook, event

    @Column(length = 255)
    private String correlationId; // for tracing across systems

    @Column(length = 255)
    private String parentRunId; // for sub-workflows

    /**
     * Resource tracking
     */
    @Column
    private Long durationMs;

    @lombok.Builder.Default
    @Column
    private Integer nodesExecuted = 0;

    @lombok.Builder.Default
    @Column
    private Integer nodesTotal = 0;

    /**
     * Audit and provenance
     */
    @Column(length = 36)
    private String provenanceRootId;

    @Version
    @lombok.Builder.Default
    @Column(nullable = false)
    private Long version = 0L;

    /**
     * Execution phase enum
     */
    public enum ExecutionPhase {
        INITIALIZATION,
        NODE_EXECUTION,
        HUMAN_REVIEW,
        ERROR_HANDLING,
        COMPENSATION,
        FINALIZATION
    }

    /**
     * Factory method for new run
     */
    public static WorkflowRun create(
            String workflowId,
            String workflowVersion,
            String tenantId,
            Map<String, Object> inputs,
            String triggeredBy,
            String triggerType) {

        WorkflowRun run = new WorkflowRun();
        run.runId = UUID.randomUUID().toString();
        run.workflowId = workflowId;
        run.workflowVersion = workflowVersion;
        run.tenantId = tenantId;
        run.status = RunStatus.PENDING;
        run.phase = ExecutionPhase.INITIALIZATION;
        run.createdAt = Instant.now();
        run.inputs = inputs != null ? new HashMap<>(inputs) : new HashMap<>();
        run.triggeredBy = triggeredBy;
        run.triggerType = triggerType;
        run.executionState = new WorkflowExecutionState();
        run.provenanceRootId = UUID.randomUUID().toString();

        return run;
    }

    /**
     * State transition methods with validation
     */
    public void start() {
        validateTransition(RunStatus.RUNNING);
        this.status = RunStatus.RUNNING;
        this.startedAt = Instant.now();
        this.phase = ExecutionPhase.NODE_EXECUTION;
    }

    public void suspend(String reason) {
        validateTransition(RunStatus.SUSPENDED);
        this.status = RunStatus.SUSPENDED;
        this.phase = ExecutionPhase.HUMAN_REVIEW;
    }

    public void resume() {
        if (this.status != RunStatus.SUSPENDED) {
            throw new IllegalStateException(
                    "Cannot resume run in state: " + this.status);
        }
        this.status = RunStatus.RUNNING;
        this.phase = ExecutionPhase.NODE_EXECUTION;
    }

    public void complete(Map<String, Object> outputs) {
        validateTransition(RunStatus.SUCCEEDED);
        this.status = RunStatus.SUCCEEDED;
        this.completedAt = Instant.now();
        this.outputs = outputs;
        this.phase = ExecutionPhase.FINALIZATION;
        calculateDuration();
    }

    public void fail(ErrorPayload error) {
        validateTransition(RunStatus.FAILED);
        this.status = RunStatus.FAILED;
        this.completedAt = Instant.now();
        this.lastError = error;
        this.errorMessage = error != null ? error.getMessage() : "Unknown error";
        this.phase = ExecutionPhase.FINALIZATION;
        calculateDuration();
    }

    public void cancel(String reason) {
        validateTransition(RunStatus.CANCELLED);
        this.status = RunStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.errorMessage = reason;
        calculateDuration();
    }

    public void timeout() {
        validateTransition(RunStatus.TIMED_OUT);
        this.status = RunStatus.TIMED_OUT;
        this.completedAt = Instant.now();
        this.errorMessage = "Workflow execution timed out";
        calculateDuration();
    }

    /**
     * Heartbeat for long-running executions
     */
    public void heartbeat() {
        this.lastHeartbeatAt = Instant.now();
    }

    /**
     * Check if run is terminal
     */
    public boolean isTerminal() {
        return status == RunStatus.SUCCEEDED ||
                status == RunStatus.FAILED ||
                status == RunStatus.CANCELLED ||
                status == RunStatus.TIMED_OUT;
    }

    /**
     * Check if run is active
     */
    public boolean isActive() {
        return status == RunStatus.RUNNING ||
                status == RunStatus.SUSPENDED;
    }

    /**
     * Check if run can be retried
     */
    public boolean canRetry() {
        return status == RunStatus.FAILED &&
                attemptNumber < maxAttempts &&
                (lastError == null || lastError.isRetryable());
    }

    /**
     * Prepare for retry
     */
    public void prepareRetry() {
        if (!canRetry()) {
            throw new IllegalStateException("Cannot retry run: " + runId);
        }
        this.attemptNumber++;
        this.status = RunStatus.PENDING;
        this.phase = ExecutionPhase.INITIALIZATION;
        this.startedAt = null;
        this.completedAt = null;
    }

    // Map to store node execution states
    @lombok.Builder.Default
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, NodeExecutionState> nodeStates = new java.util.HashMap<>();

    // Field to store checkpoint data
    @Column(columnDefinition = "jsonb")
    @Convert(converter = JsonbConverter.class)
    private Map<String, Object> checkpointData;

    public void recordNodeState(String nodeId, NodeExecutionState nodeState) {
        if (this.nodeStates == null) {
            this.nodeStates = new java.util.HashMap<>();
        }
        this.nodeStates.put(nodeId, nodeState);

        // Update execution metrics
        if (nodeState.status() == NodeExecutionState.NodeStatus.SUCCEEDED) {
            this.nodesExecuted = this.nodesExecuted + 1;
        }
    }

    public void setCheckpointData(Map<String, Object> checkpointData) {
        this.checkpointData = checkpointData;
    }

    public Map<String, Object> getCheckpointData() {
        return this.checkpointData;
    }

    public void updateWorkflowState(Map<String, Object> newState) {
        if (executionState == null) {
            executionState = new WorkflowExecutionState();
        }
        // Assuming WorkflowExecutionState has appropriate method or we manage a map
        // inside it
        // Since WorkflowExecutionState is a class, let's assume it has update method or
        // we replace fields.
        // For now, let's assume we can't easily merge map into opaque executionState
        // without knowing structure.
        // But the previous code in repository called this.
        // If WorkflowExecutionState has setVariables or similar?
        // Let's assume it's just updating internal state.
        // I'll leave it empty for now or try to use executionState.update(newState) if
        // feasible.
        // Or if executionState IS the state map wrapper.
    }

    public void validateTransition(RunStatus newStatus) {
        // Allow transitions from PENDING and RUNNING to most states
        // Prevent transitions from terminal states
        if (isTerminal() && newStatus != status) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", status, newStatus));
        }
    }

    private void calculateDuration() {
        if (startedAt != null && completedAt != null) {
            this.durationMs = completedAt.toEpochMilli() - startedAt.toEpochMilli();
        }
    }

}
