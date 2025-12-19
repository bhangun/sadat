package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record IntegrationExecutionRequest(
                String tenantId,
                String triggeredBy,
                IntegrationType integrationType,
                Map<String, Object> inputs) {
}
