package tech.kayys.wayang.mcp.model;

public enum InvocationStatus {
    SUCCESS,
    FAILURE,
    TIMEOUT,
    RATE_LIMITED,
    UNAUTHORIZED,
    VALIDATION_ERROR,
    AUTH_ERROR,
    SECURITY_VIOLATION
}