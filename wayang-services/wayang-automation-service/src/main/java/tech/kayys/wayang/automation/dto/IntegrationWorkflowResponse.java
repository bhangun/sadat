package tech.kayys.wayang.automation.dto;

public record IntegrationWorkflowResponse(
                String id,
                String name,
                IntegrationType type,
                String status) {
}