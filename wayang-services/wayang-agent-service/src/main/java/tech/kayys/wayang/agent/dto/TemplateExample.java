package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record TemplateExample(
        String name,
        String description,
        Map<String, Object> inputs,
        Map<String, Object> expectedOutputs) {
}
