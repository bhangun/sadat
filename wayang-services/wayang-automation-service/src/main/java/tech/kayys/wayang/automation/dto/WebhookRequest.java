package tech.kayys.wayang.automation.dto;

import java.util.List;

public record WebhookRequest(
                String name,
                String tenantId,
                String path,
                String method,
                String schema,
                List<WebhookAction> actions,
                String responseTemplate) {
}
