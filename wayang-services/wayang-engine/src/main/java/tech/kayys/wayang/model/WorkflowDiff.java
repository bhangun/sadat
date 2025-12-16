package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WorkflowDiff - Complete diff between two workflow versions
 * Captures all changes in logic, UI, runtime config, and metadata
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowDiff {

    // Identity
    public String baseId;
    public String targetId;
    public String baseVersion;
    public String targetVersion;
    public Instant diffTimestamp = Instant.now();

    // Component diffs
    public LogicDiff logicDiff;
    public UIDiff uiDiff;
    public RuntimeDiff runtimeDiff;
    public MetadataDiff metadataDiff;

    // Summary
    public DiffSummary summary;

    // Audit
    public String diffedBy;
    public String diffReason;

    /**
     * Check if there are any changes
     */
    public boolean hasChanges() {
        return (logicDiff != null && logicDiff.hasChanges()) ||
                (uiDiff != null && uiDiff.changed) ||
                (runtimeDiff != null && runtimeDiff.changed) ||
                (metadataDiff != null && metadataDiff.changed);
    }

    /**
     * Check if changes are breaking (require version bump)
     */
    public boolean isBreaking() {
        return logicDiff != null && (!logicDiff.nodeChanges.isEmpty() ||
                !logicDiff.connectionChanges.isEmpty() ||
                logicDiff.rulesChanged);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        WorkflowDiff that = (WorkflowDiff) o;
        return Objects.equals(baseId, that.baseId) &&
                Objects.equals(targetId, that.targetId) &&
                Objects.equals(baseVersion, that.baseVersion) &&
                Objects.equals(targetVersion, that.targetVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseId, targetId, baseVersion, targetVersion);
    }
}
