package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record ProcessExecutionRequest(
                String tenantId,
                String initiatedBy,
                Map<String, Object> processData) {
}
