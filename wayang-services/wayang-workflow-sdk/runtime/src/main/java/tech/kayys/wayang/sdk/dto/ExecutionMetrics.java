package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Execution metrics for a workflow run
 */
public record ExecutionMetrics(
    int totalNodes,
    int completedNodes,
    int failedNodes,
    int pendingNodes,
    long totalDurationMs,
    long avgNodeDurationMs
) {
    public double completionPercentage() {
        return totalNodes > 0 ? (completedNodes * 100.0) / totalNodes : 0.0;
    }
}
