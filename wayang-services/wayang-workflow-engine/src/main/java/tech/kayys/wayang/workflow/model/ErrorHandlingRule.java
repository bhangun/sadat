package tech.kayys.wayang.workflow.model;

/**
 * Error handling rule for CEL evaluation.
 */
@lombok.Data
@lombok.Builder
public class ErrorHandlingRule {
    private String name;
    private String condition; // CEL expression
    private ErrorAction action;
    private int priority;
}
