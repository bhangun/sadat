package tech.kayys.wayang.common.spi;

public interface MetricsCollector extends AutoCloseable {
    static MetricsCollector forNode(String nodeId) {
        return new NoOpMetricsCollector();
    }

    void recordExecution(long durationNs, ExecutionResult.Status status);
    void recordFailure(Throwable th);
    
    @Override
    void close();

    class NoOpMetricsCollector implements MetricsCollector {
        @Override public void recordExecution(long durationNs, ExecutionResult.Status status) {}
        @Override public void recordFailure(Throwable th) {}
        @Override public void close() {}
    }
}
