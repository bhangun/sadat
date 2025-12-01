public interface WorkerPool {
    String getId();
    CompletableFuture<ExecutionResult> execute(ExecuteNodeTask task);
    int getAvailableCapacity();
    PoolMetrics getMetrics();
    void scale(int targetSize);
}