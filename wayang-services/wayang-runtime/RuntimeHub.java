package tech.kayys.wayang.runtime;

public interface RuntimeHub {
    CompletableFuture<ExecutionResult> dispatch(ExecuteNodeTask task);
    void registerExecutor(ExecutorInfo executor);
    void unregisterExecutor(String executorId);
    List<ExecutorInfo> getAvailableExecutors();
}