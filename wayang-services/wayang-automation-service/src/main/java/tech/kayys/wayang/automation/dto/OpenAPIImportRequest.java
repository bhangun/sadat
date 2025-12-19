package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record OpenAPIImportRequest(
                String apiName,
                String tenantId,
                String openApiSpec,
                Map<String, Object> defaults) {
}
