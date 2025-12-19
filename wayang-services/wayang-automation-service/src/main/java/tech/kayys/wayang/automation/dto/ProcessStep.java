package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record ProcessStep(
                String name,
                String type,
                boolean isConditional,
                String condition,
                Map<String, Object> configuration) {
}
