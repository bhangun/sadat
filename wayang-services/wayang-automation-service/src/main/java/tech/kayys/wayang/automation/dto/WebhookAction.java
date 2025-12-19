package tech.kayys.wayang.automation.dto;

import java.util.Map;

public record WebhookAction(
                String name,
                String type,
                Map<String, Object> configuration) {
}
