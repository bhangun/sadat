package tech.kayys.wayang.workflow.version.model;

import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.version.dto.BreakingChange;
import tech.kayys.wayang.workflow.version.dto.VersionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * WorkflowVersion: Immutable version snapshot with full lineage.
 */
public class WorkflowVersion {
    private final String versionId;
    private final String workflowId;
    private final String version;
    private final String previousVersion;
    private final VersionStatus status;
    private final WorkflowDefinition definition;
    private final List<BreakingChange> breakingChanges;
    private final List<String> deprecationWarnings;
    private final Map<String, String> compatibilityMatrix;
    private final Instant createdAt;
    private final String createdBy;
    private final Instant publishedAt;
    private final String publishedBy;
    private final int canaryPercentage;
    private final String migrationPlanId;
    private final String deprecationReason;
    private final Instant deprecatedAt;
    private final Instant sunsetDate;
    private final String rollbackReason;
    private final Instant rolledBackAt;
    private final Instant archivedAt;

    private WorkflowVersion(Builder builder) {
        this.versionId = builder.versionId;
        this.workflowId = builder.workflowId;
        this.version = builder.version;
        this.previousVersion = builder.previousVersion;
        this.status = builder.status;
        this.definition = builder.definition;
        this.breakingChanges = builder.breakingChanges;
        this.deprecationWarnings = builder.deprecationWarnings;
        this.compatibilityMatrix = builder.compatibilityMatrix;
        this.createdAt = builder.createdAt;
        this.createdBy = builder.createdBy;
        this.publishedAt = builder.publishedAt;
        this.publishedBy = builder.publishedBy;
        this.canaryPercentage = builder.canaryPercentage;
        this.migrationPlanId = builder.migrationPlanId;
        this.deprecationReason = builder.deprecationReason;
        this.deprecatedAt = builder.deprecatedAt;
        this.sunsetDate = builder.sunsetDate;
        this.rollbackReason = builder.rollbackReason;
        this.rolledBackAt = builder.rolledBackAt;
        this.archivedAt = builder.archivedAt;
    }

    // Getters
    public String getVersionId() {
        return versionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getVersion() {
        return version;
    }

    public String getPreviousVersion() {
        return previousVersion;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public WorkflowDefinition getDefinition() {
        return definition;
    }

    public List<BreakingChange> getBreakingChanges() {
        return breakingChanges;
    }

    public List<String> getDeprecationWarnings() {
        return deprecationWarnings;
    }

    public Map<String, String> getCompatibilityMatrix() {
        return compatibilityMatrix;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getPublishedBy() {
        return publishedBy;
    }

    public int getCanaryPercentage() {
        return canaryPercentage;
    }

    public String getMigrationPlanId() {
        return migrationPlanId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static class Builder {
        private String versionId;
        private String workflowId;
        private String version;
        private String previousVersion;
        private VersionStatus status;
        private WorkflowDefinition definition;
        private List<BreakingChange> breakingChanges = List.of();
        private List<String> deprecationWarnings = List.of();
        private Map<String, String> compatibilityMatrix = Map.of();
        private Instant createdAt;
        private String createdBy;
        private Instant publishedAt;
        private String publishedBy;
        private int canaryPercentage = 0;
        private String migrationPlanId;
        private String deprecationReason;
        private Instant deprecatedAt;
        private Instant sunsetDate;
        private String rollbackReason;
        private Instant rolledBackAt;
        private Instant archivedAt;

        private Builder() {
        }

        private Builder(WorkflowVersion version) {
            this.versionId = version.versionId;
            this.workflowId = version.workflowId;
            this.version = version.version;
            this.previousVersion = version.previousVersion;
            this.status = version.status;
            this.definition = version.definition;
            this.breakingChanges = version.breakingChanges;
            this.deprecationWarnings = version.deprecationWarnings;
            this.compatibilityMatrix = version.compatibilityMatrix;
            this.createdAt = version.createdAt;
            this.createdBy = version.createdBy;
            this.publishedAt = version.publishedAt;
            this.publishedBy = version.publishedBy;
            this.canaryPercentage = version.canaryPercentage;
            this.migrationPlanId = version.migrationPlanId;
            this.deprecationReason = version.deprecationReason;
            this.deprecatedAt = version.deprecatedAt;
            this.sunsetDate = version.sunsetDate;
            this.rollbackReason = version.rollbackReason;
            this.rolledBackAt = version.rolledBackAt;
            this.archivedAt = version.archivedAt;
        }

        public Builder versionId(String id) {
            this.versionId = id;
            return this;
        }

        public Builder workflowId(String id) {
            this.workflowId = id;
            return this;
        }

        public Builder version(String v) {
            this.version = v;
            return this;
        }

        public Builder previousVersion(String v) {
            this.previousVersion = v;
            return this;
        }

        public Builder status(VersionStatus status) {
            this.status = status;
            return this;
        }

        public Builder definition(WorkflowDefinition def) {
            this.definition = def;
            return this;
        }

        public Builder breakingChanges(List<BreakingChange> changes) {
            this.breakingChanges = changes;
            return this;
        }

        public Builder deprecationWarnings(List<String> warnings) {
            this.deprecationWarnings = warnings;
            return this;
        }

        public Builder compatibilityMatrix(Map<String, String> matrix) {
            this.compatibilityMatrix = matrix;
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

        public Builder publishedAt(Instant at) {
            this.publishedAt = at;
            return this;
        }

        public Builder publishedBy(String by) {
            this.publishedBy = by;
            return this;
        }

        public Builder canaryPercentage(int pct) {
            this.canaryPercentage = pct;
            return this;
        }

        public Builder migrationPlanId(String id) {
            this.migrationPlanId = id;
            return this;
        }

        public Builder deprecationReason(String reason) {
            this.deprecationReason = reason;
            return this;
        }

        public Builder deprecatedAt(Instant at) {
            this.deprecatedAt = at;
            return this;
        }

        public Builder sunsetDate(Instant date) {
            this.sunsetDate = date;
            return this;
        }

        public Builder rollbackReason(String reason) {
            this.rollbackReason = reason;
            return this;
        }

        public Builder rolledBackAt(Instant at) {
            this.rolledBackAt = at;
            return this;
        }

        public Builder archivedAt(Instant at) {
            this.archivedAt = at;
            return this;
        }

        public WorkflowVersion build() {
            return new WorkflowVersion(this);
        }
    }
}