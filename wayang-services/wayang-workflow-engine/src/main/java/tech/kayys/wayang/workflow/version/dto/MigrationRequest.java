package tech.kayys.wayang.workflow.version.dto;

public record MigrationRequest(
                String fromVersion,
                String toVersion) {
}