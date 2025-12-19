package tech.kayys.wayang.automation.dto;

import java.util.List;

public record IntegrationWorkflowRequest(
        String name,
        String description,
        String tenantId,
        IntegrationType integrationType,
        TriggerSpec trigger,
        List<ConnectorSpec> connectors,
        List<TransformationSpec> transformations,
        OutputSpec output) {
}