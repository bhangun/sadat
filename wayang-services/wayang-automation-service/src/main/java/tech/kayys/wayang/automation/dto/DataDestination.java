package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record DataDestination(
                String type,
                Map<String, Object> configuration) {
}
