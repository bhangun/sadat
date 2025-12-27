package tech.kayys.wayang.workflow.version.dto;

import java.time.Instant;
import java.util.List;

/**
 * MigrationPlan: Blueprint for migrating between versions.
 */
public class MigrationPlan {
    private final String planId;
    private final String workflowId;
    private final String fromVersion;
    private final String toVersion;
    private final List<MigrationStep> steps;
    private final Instant createdAt;

    public MigrationPlan(
            String planId,
            String workflowId,
            String fromVersion,
            String toVersion,
            List<MigrationStep> steps) {
        this.planId = planId;
        this.workflowId = workflowId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.steps = steps;
        this.createdAt = Instant.now();
    }

    public String getPlanId() {
        return planId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    public List<MigrationStep> getSteps() {
        return steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}