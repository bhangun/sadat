package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record ProcessTemplate(
        String id,
        String name,
        String description,
        String industry,
        ProcessType type,
        Map<String, Object> config) {
}
