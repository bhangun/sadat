package tech.kayys.wayang.workflow.scheduler.model;

import java.time.Instant;
import java.util.Map;

import tech.kayys.wayang.workflow.scheduler.dto.MissedExecutionStrategy;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleType;

/**
 * WorkflowSchedule: Immutable domain model for workflow scheduling.
 */
public class WorkflowSchedule {
    private final String scheduleId;
    private final String workflowId;
    private final String workflowVersion;
    private final String tenantId;
    private final ScheduleType scheduleType;
    private final String cronExpression;
    private final Long interval;
    private final Instant startDate;
    private final Instant endDate;
    private final String timezone;
    private final int hour;
    private final int minute;
    private final Map<String, Object> inputs;
    private final MissedExecutionStrategy missedExecutionStrategy;
    private final boolean enabled;
    private final Instant createdAt;
    private final String createdBy;
    private final Instant lastExecutedAt;
    private final Instant nextExecutionAt;
    private final int executionCount;
    private final int consecutiveFailures;
    private final String lastError;
    private final Instant lastErrorAt;

    private WorkflowSchedule(Builder builder) {
        this.scheduleId = builder.scheduleId;
        this.workflowId = builder.workflowId;
        this.workflowVersion = builder.workflowVersion;
        this.tenantId = builder.tenantId;
        this.scheduleType = builder.scheduleType;
        this.cronExpression = builder.cronExpression;
        this.interval = builder.interval;
        this.startDate = builder.startDate;
        this.endDate = builder.endDate;
        this.timezone = builder.timezone;
        this.hour = builder.hour;
        this.minute = builder.minute;
        this.inputs = builder.inputs;
        this.missedExecutionStrategy = builder.missedExecutionStrategy;
        this.enabled = builder.enabled;
        this.createdAt = builder.createdAt;
        this.createdBy = builder.createdBy;
        this.lastExecutedAt = builder.lastExecutedAt;
        this.nextExecutionAt = builder.nextExecutionAt;
        this.executionCount = builder.executionCount;
        this.consecutiveFailures = builder.consecutiveFailures;
        this.lastError = builder.lastError;
        this.lastErrorAt = builder.lastErrorAt;
    }

    // Getters
    public String getScheduleId() {
        return scheduleId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowVersion() {
        return workflowVersion;
    }

    public String getTenantId() {
        return tenantId;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Long getInterval() {
        return interval;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public String getTimezone() {
        return timezone;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public Map<String, Object> getInputs() {
        return inputs;
    }

    public MissedExecutionStrategy getMissedExecutionStrategy() {
        return missedExecutionStrategy;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getLastExecutedAt() {
        return lastExecutedAt;
    }

    public Instant getNextExecutionAt() {
        return nextExecutionAt;
    }

    public int getExecutionCount() {
        return executionCount;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastErrorAt() {
        return lastErrorAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private String scheduleId;
        private String workflowId;
        private String workflowVersion;
        private String tenantId;
        private ScheduleType scheduleType;
        private String cronExpression;
        private Long interval;
        private Instant startDate;
        private Instant endDate;
        private String timezone = "UTC";
        private int hour = 9;
        private int minute = 0;
        private Map<String, Object> inputs = Map.of();
        private MissedExecutionStrategy missedExecutionStrategy = MissedExecutionStrategy.SKIP;
        private boolean enabled = true;
        private Instant createdAt;
        private String createdBy;
        private Instant lastExecutedAt;
        private Instant nextExecutionAt;
        private int executionCount = 0;
        private int consecutiveFailures = 0;
        private String lastError;
        private Instant lastErrorAt;

        private Builder() {
        }

        private Builder(WorkflowSchedule schedule) {
            this.scheduleId = schedule.scheduleId;
            this.workflowId = schedule.workflowId;
            this.workflowVersion = schedule.workflowVersion;
            this.tenantId = schedule.tenantId;
            this.scheduleType = schedule.scheduleType;
            this.cronExpression = schedule.cronExpression;
            this.interval = schedule.interval;
            this.startDate = schedule.startDate;
            this.endDate = schedule.endDate;
            this.timezone = schedule.timezone;
            this.hour = schedule.hour;
            this.minute = schedule.minute;
            this.inputs = schedule.inputs;
            this.missedExecutionStrategy = schedule.missedExecutionStrategy;
            this.enabled = schedule.enabled;
            this.createdAt = schedule.createdAt;
            this.createdBy = schedule.createdBy;
            this.lastExecutedAt = schedule.lastExecutedAt;
            this.nextExecutionAt = schedule.nextExecutionAt;
            this.executionCount = schedule.executionCount;
            this.consecutiveFailures = schedule.consecutiveFailures;
            this.lastError = schedule.lastError;
            this.lastErrorAt = schedule.lastErrorAt;
        }

        public Builder scheduleId(String id) {
            this.scheduleId = id;
            return this;
        }

        public Builder workflowId(String id) {
            this.workflowId = id;
            return this;
        }

        public Builder workflowVersion(String version) {
            this.workflowVersion = version;
            return this;
        }

        public Builder tenantId(String id) {
            this.tenantId = id;
            return this;
        }

        public Builder scheduleType(ScheduleType type) {
            this.scheduleType = type;
            return this;
        }

        public Builder cronExpression(String expr) {
            this.cronExpression = expr;
            return this;
        }

        public Builder interval(Long interval) {
            this.interval = interval;
            return this;
        }

        public Builder startDate(Instant date) {
            this.startDate = date;
            return this;
        }

        public Builder endDate(Instant date) {
            this.endDate = date;
            return this;
        }

        public Builder timezone(String tz) {
            this.timezone = tz;
            return this;
        }

        public Builder hour(int hour) {
            this.hour = hour;
            return this;
        }

        public Builder minute(int minute) {
            this.minute = minute;
            return this;
        }

        public Builder inputs(Map<String, Object> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder missedExecutionStrategy(MissedExecutionStrategy strategy) {
            this.missedExecutionStrategy = strategy;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder createdAt(Instant at) {
            this.createdAt = at;
            return this;
        }

        public Builder createdBy(String by) {
            this.createdBy = by;
            return this;
        }

        public Builder lastExecutedAt(Instant at) {
            this.lastExecutedAt = at;
            return this;
        }

        public Builder nextExecutionAt(Instant at) {
            this.nextExecutionAt = at;
            return this;
        }

        public Builder executionCount(int count) {
            this.executionCount = count;
            return this;
        }

        public Builder consecutiveFailures(int count) {
            this.consecutiveFailures = count;
            return this;
        }

        public Builder lastError(String error) {
            this.lastError = error;
            return this;
        }

        public Builder lastErrorAt(Instant at) {
            this.lastErrorAt = at;
            return this;
        }

        public WorkflowSchedule build() {
            return new WorkflowSchedule(this);
        }
    }
}
