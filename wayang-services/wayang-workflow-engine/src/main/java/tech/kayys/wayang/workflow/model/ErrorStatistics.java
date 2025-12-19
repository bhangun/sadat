package tech.kayys.wayang.workflow.model;

import java.util.Map;

/**
 * Error statistics summary.
 */
@lombok.Data
@lombok.Builder
public class ErrorStatistics {
    private final int totalErrors;
    private final Map<ErrorType, Long> errorsByType;
    private final Map<String, Long> errorsByNode;
    private final long retryableErrors;
}
