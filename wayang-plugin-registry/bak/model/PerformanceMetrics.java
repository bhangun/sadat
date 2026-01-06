package tech.kayys.wayang.plugin.runtime.model;

import lombok.Data;

@Data
class PerformanceMetrics {
    private double p95Latency;
    private double cpuUsagePercent;
    private double memoryUsageMb;
    private double allocatedMemoryMb;
    private double allocatedCpu;
    private double cacheHitRate;
    private double cacheSizeMb;
    private int queueDepth;
    private int threadPoolSize;
}
