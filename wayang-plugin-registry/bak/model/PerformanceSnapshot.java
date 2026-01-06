package tech.kayys.wayang.plugin.runtime.model;



import java.time.Instant;
import java.util.List;
import java.util.Map;



@lombok.Data
@lombok.Builder
class PerformanceSnapshot {
    private String pluginId;
    private String instanceId;
    private Instant timestamp;
    private PerformanceMetrics metrics;
    private Map<String, Double> deviations;
    private List<Anomaly> anomalies;
    private List<OptimizationRecommendation> recommendations;
}