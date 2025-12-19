package tech.kayys.wayang.agent.dto;

import java.time.LocalDateTime;

public record IntegrationDefinition(
        String id,
        String name,
        String description,
        String provider,
        AuthType authType,
        String config,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean isActive) {
}