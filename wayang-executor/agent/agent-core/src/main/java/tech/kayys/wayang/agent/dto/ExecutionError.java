package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Execution Error
 */
public record ExecutionError(
    String errorCode,
    String message,
    ErrorSeverity severity,
    String agentId,
    Instant occurredAt,
    Map<String, Object> context
) {}
