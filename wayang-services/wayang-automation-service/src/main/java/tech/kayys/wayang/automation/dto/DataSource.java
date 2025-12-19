package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record DataSource(
                String type,
                Map<String, Object> configuration) {
}
