package tech.kayys.wayang.workflow.version.dto;

import java.time.Instant;

public record DeprecateRequest(
                String reason,
                Instant sunsetDate) {
}