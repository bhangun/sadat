package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Publishes metrics to external monitoring systems
 */
@ApplicationScoped
public class MetricsPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsPublisher.class);

    @Inject
    ConfigurationService configService;

    private final Map<String, List<Metric>> metricBuffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService publishExecutor;
    private volatile boolean isRunning = false;

    public MetricsPublisher() {
        this.publishExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "metrics-publisher");
            t.setDaemon(true);
            return t;
        });
    }

    public Uni<Void> publishNodeMetrics(String nodeId, String nodeType,
            long durationMs, boolean success) {
        return Uni.createFrom().deferred(() -> {
            if (!configService.isMetricsPublishingEnabled()) {
                return Uni.createFrom().voidItem();
            }

            Map<String, String> tags = new HashMap<>();
            tags.put("node_id", nodeId);
            tags.put("node_type", nodeType);
            tags.put("success", Boolean.toString(success));

            Metric durationMetric = new Metric(
                    "node_execution_duration_ms",
                    (double) durationMs,
                    tags,
                    Instant.now());

            Metric successMetric = new Metric(
                    "node_execution_success",
                    success ? 1.0 : 0.0,
                    tags,
                    Instant.now());

            bufferMetric(durationMetric);
            bufferMetric(successMetric);

            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> publishError(String nodeId, String nodeType, String errorType) {
        return Uni.createFrom().deferred(() -> {
            if (!configService.isMetricsPublishingEnabled()) {
                return Uni.createFrom().voidItem();
            }

            Map<String, String> tags = new HashMap<>();
            tags.put("node_id", nodeId);
            tags.put("node_type", nodeType);
            tags.put("error_type", errorType);

            Metric errorMetric = new Metric(
                    "node_execution_error",
                    1.0,
                    tags,
                    Instant.now());

            bufferMetric(errorMetric);

            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> publishCustomMetric(String metricName, double value,
            Map<String, String> tags) {
        return Uni.createFrom().deferred(() -> {
            if (!configService.isMetricsPublishingEnabled()) {
                return Uni.createFrom().voidItem();
            }

            Metric metric = new Metric(
                    metricName,
                    value,
                    tags != null ? new HashMap<>(tags) : new HashMap<>(),
                    Instant.now());

            bufferMetric(metric);

            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> publishRunMetrics(Object runMetrics) {
        return Uni.createFrom().deferred(() -> {
            // Implementation would convert run metrics to appropriate format
            // and publish them
            LOG.debug("Publishing run metrics: {}", runMetrics);
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> publishBatchMetrics(BatchMetrics metrics) {
        return Uni.createFrom().deferred(() -> {
            Map<String, String> tags = new HashMap<>();
            tags.put("batch_id", metrics.getBatchId());
            tags.put("total_nodes", Integer.toString(metrics.getTotalNodes()));

            Metric durationMetric = new Metric(
                    "batch_execution_duration_ms",
                    metrics.getDurationMs(),
                    tags,
                    Instant.now());

            Metric successRateMetric = new Metric(
                    "batch_success_rate",
                    metrics.getSuccessRate(),
                    tags,
                    Instant.now());

            bufferMetric(durationMetric);
            bufferMetric(successRateMetric);

            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> startPeriodicPublishing() {
        return Uni.createFrom().deferred(() -> {
            if (isRunning) {
                return Uni.createFrom().voidItem();
            }

            isRunning = true;
            publishExecutor.scheduleAtFixedRate(() -> {
                try {
                    flushMetrics();
                } catch (Exception e) {
                    LOG.error("Error in metrics publishing", e);
                }
            }, 10, 10, TimeUnit.SECONDS);

            LOG.info("Periodic metrics publishing started");
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> stopPeriodicPublishing() {
        return Uni.createFrom().deferred(() -> {
            isRunning = false;
            publishExecutor.shutdown();
            LOG.info("Periodic metrics publishing stopped");
            return Uni.createFrom().voidItem();
        });
    }

    public Uni<Void> flush() {
        return Uni.createFrom().deferred(this::flushMetrics);
    }

    public Uni<Map<String, Object>> getPublisherStats() {
        return Uni.createFrom().deferred(() -> {
            Map<String, Object> stats = new HashMap<>();

            int totalBuffered = metricBuffer.values().stream()
                    .mapToInt(List::size)
                    .sum();

            stats.put("bufferedMetrics", totalBuffered);
            stats.put("isRunning", isRunning);
            stats.put("timestamp", Instant.now().toString());

            return Uni.createFrom().item(stats);
        });
    }

    private void bufferMetric(Metric metric) {
        String metricName = metric.getName();
        metricBuffer.computeIfAbsent(metricName, k -> new ArrayList<>())
                .add(metric);

        // If buffer gets too large, trigger immediate flush
        if (metricBuffer.get(metricName).size() > 1000) {
            publishExecutor.execute(this::flushMetrics);
        }
    }

    private Uni<Void> flushMetrics() {
        return Uni.createFrom().deferred(() -> {
            if (metricBuffer.isEmpty()) {
                return Uni.createFrom().voidItem();
            }

            // Group metrics by name for efficient publishing
            Map<String, List<Metric>> metricsToPublish = new HashMap<>(metricBuffer);
            metricBuffer.clear();

            List<Uni<Void>> publishUnis = new ArrayList<>();

            for (Map.Entry<String, List<Metric>> entry : metricsToPublish.entrySet()) {
                String metricName = entry.getKey();
                List<Metric> metrics = entry.getValue();

                // Aggregate metrics if possible
                if (shouldAggregate(metricName)) {
                    List<Metric> aggregated = aggregateMetrics(metrics);
                    publishUnis.add(publishToBackend(metricName, aggregated));
                } else {
                    publishUnis.add(publishToBackend(metricName, metrics));
                }
            }

            return Uni.combine().all().unis(publishUnis).asList()
                    .onItem().invoke(results -> LOG.debug("Flushed {} metric groups", results.size()))
                    .replaceWithVoid();
        });
    }

    private boolean shouldAggregate(String metricName) {
        // Aggregate high-frequency metrics
        return metricName.contains("duration") ||
                metricName.contains("success") ||
                metricName.contains("error");
    }

    private List<Metric> aggregateMetrics(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            return List.of();
        }

        // Group by tags
        Map<String, List<Metric>> groupedByTags = new HashMap<>();

        for (Metric metric : metrics) {
            String tagKey = metric.getTags().toString(); // Simple grouping
            groupedByTags.computeIfAbsent(tagKey, k -> new ArrayList<>())
                    .add(metric);
        }

        List<Metric> aggregated = new ArrayList<>();

        for (List<Metric> group : groupedByTags.values()) {
            if (group.size() == 1) {
                aggregated.add(group.get(0));
            } else {
                // Calculate average for duration metrics
                double sum = group.stream().mapToDouble(Metric::getValue).sum();
                double average = sum / group.size();

                // Use timestamp of last metric
                Instant timestamp = group.get(group.size() - 1).getTimestamp();

                // Use tags from first metric
                Map<String, String> tags = group.get(0).getTags();
                tags.put("aggregated", "true");
                tags.put("sample_count", Integer.toString(group.size()));

                aggregated.add(new Metric(
                        group.get(0).getName(),
                        average,
                        tags,
                        timestamp));
            }
        }

        return aggregated;
    }

    private Uni<Void> publishToBackend(String metricName, List<Metric> metrics) {
        return Uni.createFrom().deferred(() -> {
            if (metrics.isEmpty()) {
                return Uni.createFrom().voidItem();
            }

            // Determine backend based on configuration
            String backend = configService.getMetricsBackend();

            return switch (backend) {
                case "prometheus" -> publishToPrometheus(metricName, metrics);
                case "datadog" -> publishToDatadog(metricName, metrics);
                case "cloudwatch" -> publishToCloudWatch(metricName, metrics);
                default -> publishToLog(metricName, metrics);
            };
        });
    }

    private Uni<Void> publishToPrometheus(String metricName, List<Metric> metrics) {
        // Simulated Prometheus publishing
        LOG.trace("Publishing {} metrics to Prometheus: {}", metrics.size(), metricName);
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> publishToDatadog(String metricName, List<Metric> metrics) {
        // Simulated Datadog publishing
        LOG.trace("Publishing {} metrics to Datadog: {}", metrics.size(), metricName);
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> publishToCloudWatch(String metricName, List<Metric> metrics) {
        // Simulated CloudWatch publishing
        LOG.trace("Publishing {} metrics to CloudWatch: {}", metrics.size(), metricName);
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> publishToLog(String metricName, List<Metric> metrics) {
        // Fallback to logging
        if (LOG.isDebugEnabled()) {
            for (Metric metric : metrics) {
                LOG.debug("Metric: {}={} tags={}",
                        metric.getName(), metric.getValue(), metric.getTags());
            }
        }
        return Uni.createFrom().voidItem();
    }

    public static class Metric {
        private final String name;
        private final double value;
        private final Map<String, String> tags;
        private final Instant timestamp;

        public Metric(String name, double value, Map<String, String> tags, Instant timestamp) {
            this.name = name;
            this.value = value;
            this.tags = Map.copyOf(tags);
            this.timestamp = timestamp;
        }

        // Getters...
        public String getName() {
            return name;
        }

        public double getValue() {
            return value;
        }

        public Map<String, String> getTags() {
            return tags;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }

    public static class BatchMetrics {
        private final String batchId;
        private final int totalNodes;
        private final double durationMs;
        private final double successRate;

        public BatchMetrics(String batchId, int totalNodes, double durationMs, double successRate) {
            this.batchId = batchId;
            this.totalNodes = totalNodes;
            this.durationMs = durationMs;
            this.successRate = successRate;
        }

        // Getters...
        public String getBatchId() {
            return batchId;
        }

        public int getTotalNodes() {
            return totalNodes;
        }

        public double getDurationMs() {
            return durationMs;
        }

        public double getSuccessRate() {
            return successRate;
        }
    }
}