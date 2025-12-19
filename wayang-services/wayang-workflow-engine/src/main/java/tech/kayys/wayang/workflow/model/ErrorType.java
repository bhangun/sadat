package tech.kayys.wayang.workflow.model;

/**
 * Error type enum.
 */
public enum ErrorType {
    TOOL_ERROR,
    LLM_ERROR,
    NETWORK_ERROR,
    VALIDATION_ERROR,
    TIMEOUT,
    PLUGIN_LOAD_ERROR,
    SECURITY_ERROR,
    UNKNOWN_ERROR
}
