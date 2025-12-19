package tech.kayys.wayang.workflow.model;

/**
 * Run statistics.
 */
@lombok.Data
@lombok.Builder
public class RunStatistics {
    private Long totalRuns;
    private Long completedRuns;
    private Long failedRuns;
    private Long runningRuns;
    private Double avgDurationSeconds;
}
