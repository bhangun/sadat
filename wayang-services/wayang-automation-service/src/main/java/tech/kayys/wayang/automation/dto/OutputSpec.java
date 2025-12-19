package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record OutputSpec(
                String destination,
                String format,
                Map<String, Object> configuration) {
}
