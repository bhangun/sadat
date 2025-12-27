package tech.kayys.wayang.workflow.version.dto;

public record CreateVersionRequest(
                String version,
                String previousVersion,
                String createdBy) {
}