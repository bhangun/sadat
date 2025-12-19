package tech.kayys.wayang.workflow.model.saga;

import java.util.Map;

/**
 * Compensation Action
 */
public record CompensationAction(
        String actionType,
        Map<String, Object> parameters) {
}