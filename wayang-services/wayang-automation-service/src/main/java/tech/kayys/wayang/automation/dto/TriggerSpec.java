package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record TriggerSpec(
                TriggerType type,
                String schedule,
                Map<String, Object> configuration) {
}
