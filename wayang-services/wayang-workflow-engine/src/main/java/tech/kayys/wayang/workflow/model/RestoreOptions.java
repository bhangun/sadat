package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.List;

@lombok.Builder
public class RestoreOptions {
    private RestoreStrategy strategy;
    private Instant targetTimestamp;
    private List<String> selectedTenants;

    public enum RestoreStrategy {
        FULL_RESTORE,
        POINT_IN_TIME,
        SELECTIVE
    }

    public RestoreStrategy getStrategy() {
        return strategy;
    }

    public Instant getTargetTimestamp() {
        return targetTimestamp;
    }

    public List<String> getSelectedTenants() {
        return selectedTenants;
    }
}
