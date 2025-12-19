package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record WorkflowDefinition(
        String id,
        String name,
        String description,
        String tenantId,
        String status,
        long createdAt,
        long updatedAt,
        Map<String, Object> config) {
}