package tech.kayys.wayang.automation.dto;

import java.time.Instant;

public record IntegrationExecutionResponse(
                String executionId,
                String status,
                Instant createdAt) {
}