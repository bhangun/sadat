package tech.kayys.wayang.workflow.version.dto;

public record VersionRequest(
        String workflowId,
        String version,
        String previousVersion,
        String createdBy) {
}
