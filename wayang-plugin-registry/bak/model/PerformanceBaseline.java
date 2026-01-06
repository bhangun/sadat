package tech.kayys.wayang.plugin.runtime.model;

import java.time.Duration;
import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
class PerformanceBaseline {
    private String pluginId;
    private Duration period;
    private Instant establishedAt;
    private double p50Latency;
    private double p95Latency;
    private double p99Latency;
    private double avgCpu;
    private double avgMemory;
    private double avgThroughput;
    private double errorRate;
}
