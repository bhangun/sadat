package tech.kayys.wayang.workflow.scheduler.dto;

import java.time.Instant;
import java.util.Map;

/**
 * ScheduleExecution: Record of a schedule execution attempt.
 */
public class ScheduleExecution {
    private final String executionId;
    private final String scheduleId;
    private final String workflowId;
    private final String tenantId;
    private final Instant scheduledFor;
    private final Instant startedAt;
    private final Instant completedAt;
    private final ExecutionStatus status;
    private final String runId;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    private ScheduleExecution(Builder builder) {
        this.executionId = builder.executionId;
        this.scheduleId = builder.scheduleId;
        this.workflowId = builder.workflowId;
        this.tenantId = builder.tenantId;
        this.scheduledFor = builder.scheduledFor;
        this.startedAt = builder.startedAt;
        this.completedAt = builder.completedAt;
        this.status = builder.status;
        this.runId = builder.runId;
        this.errorMessage = builder.errorMessage;
        this.metadata = builder.metadata;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getScheduleId() {
        return scheduleId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Instant getScheduledFor() {
        return scheduledFor;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public String getRunId() {
        return runId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private String executionId;
        private String scheduleId;
        private String workflowId;
        private String tenantId;
        private Instant scheduledFor;
        private Instant startedAt;
        private Instant completedAt;
        private ExecutionStatus status;
        private String runId;
        private String errorMessage;
        private Map<String, Object> metadata = Map.of();

        private Builder() {
        }

        private Builder(ScheduleExecution exec) {
            this.executionId = exec.executionId;
            this.scheduleId = exec.scheduleId;
            this.workflowId = exec.workflowId;
            this.tenantId = exec.tenantId;
            this.scheduledFor = exec.scheduledFor;
            this.startedAt = exec.startedAt;
            this.completedAt = exec.completedAt;
            this.status = exec.status;
            this.runId = exec.runId;
            this.errorMessage = exec.errorMessage;
            this.metadata = exec.metadata;
        }

        public Builder executionId(String id) {
            this.executionId = id;
            return this;
        }

        public Builder scheduleId(String id) {
            this.scheduleId = id;
            return this;
        }

        public Builder workflowId(String id) {
            this.workflowId = id;
            return this;
        }

        public Builder tenantId(String id) {
            this.tenantId = id;
            return this;
        }

        public Builder scheduledFor(Instant at) {
            this.scheduledFor = at;
            return this;
        }

        public Builder startedAt(Instant at) {
            this.startedAt = at;
            return this;
        }

        public Builder completedAt(Instant at) {
            this.completedAt = at;
            return this;
        }

        public Builder status(ExecutionStatus status) {
            this.status = status;
            return this;
        }

        public Builder runId(String id) {
            this.runId = id;
            return this;
        }

        public Builder errorMessage(String msg) {
            this.errorMessage = msg;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ScheduleExecution build() {
            return new ScheduleExecution(this);
        }
    }
}
