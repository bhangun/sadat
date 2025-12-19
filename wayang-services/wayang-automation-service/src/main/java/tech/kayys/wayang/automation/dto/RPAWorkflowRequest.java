package tech.kayys.wayang.automation.dto;

import java.util.List;

public record RPAWorkflowRequest(
                String name,
                String tenantId,
                List<RPAAction> actions) {
}
