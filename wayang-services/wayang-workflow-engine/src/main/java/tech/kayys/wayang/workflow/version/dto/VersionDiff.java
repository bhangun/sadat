package tech.kayys.wayang.workflow.version.dto;

import java.util.List;

public record VersionDiff(
        String fromVersion,
        String toVersion,
        List<BreakingChange> breakingChanges,
        List<String> additions) {
}