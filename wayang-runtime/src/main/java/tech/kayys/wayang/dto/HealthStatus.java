package tech.kayys.wayang.dto;

import java.time.Instant;
import java.util.Map;

public record HealthStatus(
        String status,
        Instant timestamp,
        Map<String, Object> details) {
}