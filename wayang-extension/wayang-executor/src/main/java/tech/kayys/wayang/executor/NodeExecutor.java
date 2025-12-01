package tech.kayys.wayang.executor;

public interface NodeExecutor {
    CompletableFuture<ExecutionResult> execute(ExecuteNodeTask task);
    void cancel(String taskId);
    TaskStatus getStatus(String taskId);
}