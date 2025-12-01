
/**
 * Node performance metrics
 */
class NodePerformanceMetrics {
    final String nodeId;
    final AtomicLong successCount = new AtomicLong(0);
    final AtomicLong failureCount = new AtomicLong(0);
    final AtomicLong totalDurationMs = new AtomicLong(0);
    final AtomicLong minDurationMs = new AtomicLong(Long.MAX_VALUE);
    final AtomicLong maxDurationMs = new AtomicLong(0);
    volatile Instant lastExecutionTime;
    
    NodePerformanceMetrics(String nodeId) {
        this.nodeId = nodeId;
    }
}
