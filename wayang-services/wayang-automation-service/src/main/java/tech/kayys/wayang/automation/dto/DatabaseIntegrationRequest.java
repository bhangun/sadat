package tech.kayys.wayang.automation.dto;

public record DatabaseIntegrationRequest(
                String name,
                String tenantId,
                String databaseType,
                String connectionString,
                String credentialsRef,
                String query,
                String resultTransformation) {
}
