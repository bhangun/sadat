package tech.kayys.wayang.workflow.kernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.sdk.dto.ExecutionMetrics;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import tech.kayys.wayang.workflow.executor.NodeExecutor;
import tech.kayys.wayang.workflow.executor.NodeExecutorRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base implementation with shared logic
 */
@ApplicationScoped
public abstract class AbstractWorkflowEngine
        extends TenantAwareComponent
        implements InstrumentedWorkflowEngine {

    private static final Logger LOG = Logger.getLogger(AbstractWorkflowEngine.class);

    @Inject
    protected ExecutionMetricsCollector metricsCollector;

    @Inject
    protected NodeExecutorRegistry executorRegistry;

    protected final Map<String, NodeExecutionStats> statsCache = new ConcurrentHashMap<>();
    protected volatile HealthStatus healthStatus = HealthStatus.HEALTHY;
    protected volatile ComponentConfig config;

    @Override
    public Uni<NodeExecutionResult> executeNode(
            ExecutionContext context,
            NodeDescriptor node,
            ExecutionToken token) {

        return validateNotEmpty("context", context != null ? context.getWorkflowRunId() : null,
                validateNotEmpty("nodeId", node != null ? node.getId() : null,
                        validateNotNull("token", token, Uni.createFrom().deferred(() -> {

                            // Verify token is valid and not expired
                            if (token.isExpired()) {
                                return Uni.createFrom().failure(
                                        new SecurityException("Execution token expired"));
                            }

                            // Get appropriate executor for node type
                            NodeExecutor executor = executorRegistry.getExecutor(node.getType());
                            if (executor == null) {
                                return Uni.createFrom().failure(
                                        new IllegalArgumentException(
                                                "No executor found for node type: " + node.getType()));
                            }

                            LOG.debugf("Executing node %s for run %s", node.getId(), context.getWorkflowRunId());

                            // Create safe context copy
                            ExecutionContext safeContext = createSafeExecutionContext(context);

                            // Record start time for metrics
                            long startTime = System.nanoTime();

                            return executor.execute(safeContext, node)
                                    .onItem().transform(result -> {
                                        // Record metrics
                                        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                                        recordExecutionMetrics(node, durationMs, result.getStatus());

                                        // Update cache
                                        updateStatsCache(node.getType(), durationMs, result.getStatus());

                                        return result;
                                    })
                                    .onFailure().recoverWithUni(th -> {
                                        LOG.errorf(th, "Node execution failed: %s", node.getId());
                                        recordExecutionFailure(node, th);
                                        return Uni.createFrom().failure(th);
                                    });
                        }))));
    }

    @Override
    public Uni<NodeExecutionResult> dryRunNode(
            ExecutionContext context,
            NodeDescriptor node) {

        return validateNotNull("context", context,
                validateNotNull("node", node, Uni.createFrom().deferred(() -> {

                    NodeExecutor executor = executorRegistry.getExecutor(node.getType());
                    if (executor == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("No executor found for node type: " + node.getType()));
                    }

                    ExecutionContext safeContext = createSafeExecutionContext(context);

                    return executor.dryRun(safeContext, node)
                            .onFailure().recoverWithUni(th -> {
                                LOG.warnf(th, "Dry run failed for node %s", node.getId());
                                return Uni.createFrom().item(NodeExecutionResult.failure(
                                        node.getId(), "Dry run failed: " + th.getMessage()));
                            });
                })));
    }

    @Override
    public Uni<ValidationResult> validateExecution(
            ExecutionContext context,
            NodeDescriptor node) {

        return Uni.createFrom().deferred(() -> {
            List<String> errors = new ArrayList<>();

            // Basic validation
            if (context == null) {
                errors.add("Context cannot be null");
            }

            if (node == null) {
                errors.add("Node cannot be null");
            } else {
                // Validate node configuration
                if (node.getType() == null || node.getType().isEmpty()) {
                    errors.add("Node type cannot be empty");
                }

                // Check if executor exists
                NodeExecutor executor = executorRegistry.getExecutor(node.getType());
                if (executor == null) {
                    errors.add("No executor available for node type: " + node.getType());
                }
            }

            if (errors.isEmpty()) {
                return Uni.createFrom().item(ValidationResult.success());
            } else {
                return Uni.createFrom().item(
                        ValidationResult.failure(String.join("; ", errors)));
            }
        });
    }

    @Override
    public Uni<InstrumentedNodeExecutionResult> executeNodeWithTelemetry(
            ExecutionContext context,
            NodeDescriptor node,
            ExecutionToken token,
            TelemetryConfig telemetryConfig) {

        return executeNode(context, node, token)
                .onItem().transform(result -> {
                    ExecutionMetrics metrics = metricsCollector.collectForNode(node.getId());
                    List<PerformanceMarker> markers = telemetryConfig.enableTracing() ? collectPerformanceMarkers()
                            : List.of();

                    return new InstrumentedNodeExecutionResult(result, metrics, markers);
                });
    }

    @Override
    public Uni<NodeExecutionStats> getNodeExecutionStats(String nodeType) {
        return Uni.createFrom().item(() -> {
            NodeExecutionStats stats = statsCache.get(nodeType);
            if (stats == null) {
                stats = new NodeExecutionStats(nodeType, 0, 0, 0.0, Map.of());
            }
            return stats;
        });
    }

    @Override
    public Uni<Void> initialize(ComponentConfig config) {
        return Uni.createFrom().deferred(() -> {
            this.config = config;
            LOG.infof("Initializing WorkflowEngine with config: %s", config.componentName());
            return Uni.createFrom().voidItem();
        });
    }

    @Override
    public Uni<Void> start() {
        return Uni.createFrom().deferred(() -> {
            healthStatus = HealthStatus.HEALTHY;
            LOG.info("WorkflowEngine started");
            return Uni.createFrom().voidItem();
        });
    }

    @Override
    public Uni<Void> stop() {
        return Uni.createFrom().deferred(() -> {
            healthStatus = HealthStatus.UNHEALTHY;
            LOG.info("WorkflowEngine stopped");
            return Uni.createFrom().voidItem();
        });
    }

    @Override
    public Uni<HealthStatus> healthCheck() {
        return Uni.createFrom().item(() -> healthStatus);
    }

    @Override
    public Uni<ComponentMetrics> getMetrics() {
        return Uni.createFrom().deferred(() -> {
            ComponentMetrics metrics = new ComponentMetrics(
                    metricsCollector.getTotalRequests(),
                    metricsCollector.getSuccessfulRequests(),
                    metricsCollector.getFailedRequests(),
                    metricsCollector.getAverageLatencyMs(),
                    Map.of("statsCacheSize", statsCache.size()));
            return Uni.createFrom().item(metrics);
        });
    }

    @Override
    public void recordMetric(String metricName, double value, Map<String, String> tags) {
        metricsCollector.recordCustomMetric(metricName, value, tags);
    }

    // Protected helper methods
    protected void recordExecutionMetrics(NodeDescriptor node, long durationMs,
            NodeExecutionStatus status) {
        metricsCollector.recordNodeExecution(
                node.getId(),
                node.getType(),
                durationMs,
                status == NodeExecutionStatus.SUCCESS);
    }

    protected void recordExecutionFailure(NodeDescriptor node, Throwable throwable) {
        metricsCollector.recordFailure(node.getId(), node.getType(), throwable);
    }

    protected void updateStatsCache(String nodeType, long durationMs, NodeExecutionStatus status) {
        statsCache.compute(nodeType, (key, existing) -> {
            if (existing == null) {
                Map<String, Long> errorCounts = status == NodeExecutionStatus.FAILED ? Map.of("total", 1L) : Map.of();
                return new NodeExecutionStats(
                        nodeType, 1,
                        status == NodeExecutionStatus.SUCCESS ? 1 : 0,
                        durationMs, errorCounts);
            } else {
                long newTotal = existing.totalExecutions() + 1;
                long newSuccessful = existing.successfulExecutions() +
                        (status == NodeExecutionStatus.SUCCESS ? 1 : 0);
                double newAvg = (existing.averageDurationMs() * existing.totalExecutions() + durationMs) / newTotal;

                Map<String, Long> newErrorCounts = new HashMap<>(existing.errorCounts());
                if (status == NodeExecutionStatus.FAILED) {
                    newErrorCounts.merge("total", 1L, Long::sum);
                }

                return new NodeExecutionStats(
                        nodeType, newTotal, newSuccessful, newAvg, newErrorCounts);
            }
        });
    }

    protected List<PerformanceMarker> collectPerformanceMarkers() {
        // Implementation for collecting performance markers
        return List.of(
                new PerformanceMarker("engine_processing_start", System.nanoTime()),
                new PerformanceMarker("engine_processing_end", System.nanoTime()));
    }

    public record PerformanceMarker(String name, long timestampNanos) {
    }
}
