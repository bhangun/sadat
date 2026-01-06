package tech.kayys.wayang.project.dto;

public record PublishResponse(
        boolean success,
        String workflowDefinitionId,
        String message) {
}
