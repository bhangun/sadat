package tech.kayys.wayang.project.dto;

import java.time.Instant;

public record ExecutionResponse(
        String runId,
        String status,
        Instant startedAt) {
}
