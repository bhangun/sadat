package tech.kayys.wayang.workflow.version.dto;

public record BreakingChange(
        String nodeId,
        ChangeType type,
        String description) {
}
