package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Workflow snapshot for performance
 */
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Workflow snapshot for performance
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSnapshot {
    private String id;
    private String runId;
    private WorkflowRun state;
    private long eventCount;
    private Instant createdAt;
    private Instant lastModified;

    public WorkflowSnapshot(String runId, WorkflowRun state, long eventCount, Instant createdAt) {
        this.id = java.util.UUID.randomUUID().toString();
        this.runId = runId;
        this.state = state;
        this.eventCount = eventCount;
        this.createdAt = createdAt;
        this.lastModified = createdAt;
    }

    public String runId() {
        return runId;
    }

    public WorkflowRun state() {
        return state;
    }

    public WorkflowRun workflowRun() {
        return state;
    }

    public long eventCount() {
        return eventCount;
    }

    public Instant createdAt() {
        return createdAt;
    }
}