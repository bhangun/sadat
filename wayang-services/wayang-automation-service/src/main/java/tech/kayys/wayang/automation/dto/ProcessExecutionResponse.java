package tech.kayys.wayang.automation.dto;

import java.time.Instant;

public record ProcessExecutionResponse(
                String executionId,
                String status,
                String currentStep,
                Instant startedAt) {
}