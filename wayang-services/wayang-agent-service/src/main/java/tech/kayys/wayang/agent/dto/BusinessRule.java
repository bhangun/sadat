package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;

public record BusinessRule(
        String id,
        String name,
        String description,
        String condition,
        String action,
        String priority,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive) {
}