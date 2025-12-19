package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record NodeSpec(
        String type,
        String name,
        Map<String, Object> config) {
}