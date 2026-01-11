package tech.kayys.silat.api.subworkflow;

record CascadeCancellationResult(
    boolean success,
    int workflowsCancelled,
    String reason,
    java.time.Instant cancelledAt
) {}