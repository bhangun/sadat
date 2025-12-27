package tech.kayys.wayang.workflow.version.dto;

import java.util.Map;

public record MigrationStep(
        int order,
        String description,
        String type,
        Map<String, Object> parameters) {
}