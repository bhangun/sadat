package tech.kayys.wayang.workflow.saga.model;

import java.util.Map;

/**
 * Compensation Action
 */
public record CompensationAction(
                String actionType,
                Map<String, Object> parameters) {
}