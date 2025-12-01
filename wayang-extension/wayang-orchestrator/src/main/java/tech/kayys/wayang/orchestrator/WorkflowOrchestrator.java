package tech.kayys.wayang.orchestrator;

public interface WorkflowOrchestrator {
    CompletableFuture<ExecutionRun> execute(ExecutionPlan plan, ExecutionRequest request);
    CompletableFuture<ExecutionRun> resume(UUID runId, String checkpointRef);
    void cancel(UUID runId);
    ExecutionRun getRunStatus(UUID runId);
    void pause(UUID runId);
}