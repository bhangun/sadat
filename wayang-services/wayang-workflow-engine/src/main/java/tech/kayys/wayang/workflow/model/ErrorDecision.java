package tech.kayys.wayang.workflow.model;

import java.util.Map;

/**
 * Error decision result.
 */
@lombok.Data
@lombok.Builder
public class ErrorDecision {
    private final ErrorAction action;
    private final String reason;
    private final Map<String, Object> metadata;
}