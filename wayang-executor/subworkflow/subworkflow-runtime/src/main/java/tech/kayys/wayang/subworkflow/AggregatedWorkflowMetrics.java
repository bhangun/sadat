package tech.kayys.silat.api.subworkflow;

record AggregatedWorkflowMetrics(
    String rootRunId,
    int totalWorkflows,
    int completedWorkflows,
    long averageDurationMs
) {}