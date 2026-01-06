package tech.kayys.wayang.workflow.kernel;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Hints for workflow execution (optional optimizations).
 */
@Data
@Builder
public class ExecutionHints {
    private final String preferredStrategy;
    private final boolean parallelizable;
    private final boolean cacheable;
    private final Map<String, Object> optimizations;
}