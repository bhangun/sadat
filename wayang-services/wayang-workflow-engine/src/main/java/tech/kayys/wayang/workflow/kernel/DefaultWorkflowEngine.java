package tech.kayys.wayang.workflow.kernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.sdk.dto.ExecutionMetrics;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of WorkflowEngine using the abstract base
 */

@ApplicationScoped
public class DefaultWorkflowEngine extends AbstractWorkflowEngine {

    private static final Logger LOG = Logger.getLogger(DefaultWorkflowEngine.class);

    @Inject
    RetryPolicyManager retryPolicyManager;

    @Inject
    ExecutionContextEnricher contextEnricher;

    @Inject
    ExecutionTokenValidator tokenValidator;

    @Inject
    ExecutionMetricsCollector metricsCollector;

    @Inject
    ClusterCoordinator clusterCoordinator;

    // New method for batch execution
    @Override
    public Uni<BatchExecutionResult> executeNodes(
            List<NodeExecutionRequest> requests,
            ExecutionContext sharedContext) {

        return Uni.createFrom().deferred(() -> {
            String batchId = "batch-" + UUID.randomUUID().toString().substring(0, 8);
            LOG.infof("Executing batch %s with %d requests", batchId, requests.size());

            List<Uni<BatchExecutionResult.IndividualResult>> resultUnis = requests.stream()
                    .map(request -> executeNodeInBatch(request, sharedContext))
                    .toList();

            return Uni.combine().all().unis(resultUnis).asList()
                    .map(results -> {
                        boolean allSuccess = results.stream()
                                .allMatch(BatchExecutionResult.IndividualResult::isSuccess);
                        boolean anySuccess = results.stream()
                                .anyMatch(BatchExecutionResult.IndividualResult::isSuccess);

                        BatchExecutionResult.BatchStatus status;
                        if (allSuccess) {
                            status = BatchExecutionResult.BatchStatus.COMPLETED;
                        } else if (anySuccess) {
                            status = BatchExecutionResult.BatchStatus.PARTIAL_SUCCESS;
                        } else {
                            status = BatchExecutionResult.BatchStatus.FAILED;
                        }

                        return new BatchExecutionResult(
                                batchId,
                                results,
                                status,
                                Map.of(
                                        "totalRequests", requests.size(),
                                        "sharedContextId", sharedContext.getExecutionId()));
                    });
        });
    }

    private Uni<BatchExecutionResult.IndividualResult> executeNodeInBatch(
            NodeExecutionRequest request,
            ExecutionContext sharedContext) {

        // Merge shared context with request context
        ExecutionContext mergedContext = mergeContexts(sharedContext, request.getContext());

        return executeNode(mergedContext, request.getNode(), request.getToken())
                .map(result -> new BatchExecutionResult.IndividualResult(
                        request.getRequestId(),
                        result,
                        null,
                        System.currentTimeMillis() // Would need actual timing
                ))
                .onFailure().recoverWithItem(th -> new BatchExecutionResult.IndividualResult(
                        request.getRequestId(),
                        NodeExecutionResult.failure(request.getNode().getId(), th.getMessage()),
                        th,
                        System.currentTimeMillis()));
    }

    @Override
    public ResourceEstimate estimateResources(
            ExecutionContext context,
            NodeDescriptor node) {

        // Get historical data
        ExecutionMetrics metrics = metricsCollector.collectForNode(node.getNodeId());

        // Calculate resource estimate based on node type and historical performance
        double cpuCores = estimateCpuRequirement(node, metrics);
        long memoryBytes = estimateMemoryRequirement(node, metrics);
        long storageBytes = estimateStorageRequirement(node);

        ResourceEstimate.CpuRequirement cpu = new ResourceEstimate.CpuRequirement(
                cpuCores, ResourceEstimate.Unit.CORES);
        ResourceEstimate.MemoryRequirement memory = new ResourceEstimate.MemoryRequirement(memoryBytes);
        ResourceEstimate.StorageRequirement storage = new ResourceEstimate.StorageRequirement(storageBytes,
                ResourceEstimate.StorageType.SSD);
        ResourceEstimate.NetworkRequirement network = new ResourceEstimate.NetworkRequirement(1000000, // 1 Mbps default
                new ResourceEstimate.LatencyRequirement(100, 10));

        ResourceEstimate.ConfidenceLevel confidence = calculateConfidenceLevel(metrics);

        return new ResourceEstimate(
                cpu, memory, storage, network,
                node.getConfig() != null ? new HashMap<>(node.getConfig()) : Map.of(),
                confidence);
    }

    private double estimateCpuRequirement(NodeDescriptor node, ExecutionMetrics metrics) {
        // Simple estimation based on node type
        return switch (node.getType()) {
            case "compute-intensive" -> 2.0;
            case "io-intensive" -> 0.5;
            case "memory-intensive" -> 1.0;
            default -> 0.1;
        };
    }

    private long estimateMemoryRequirement(NodeDescriptor node, ExecutionMetrics metrics) {
        return switch (node.getType()) {
            case "memory-intensive" -> 1024 * 1024 * 1024L; // 1GB
            case "compute-intensive" -> 512 * 1024 * 1024L; // 512MB
            default -> 256 * 1024 * 1024L; // 256MB
        };
    }

    private long estimateStorageRequirement(NodeDescriptor node) {
        // Check if node has storage requirements in metadata
        if (node.getConfig() != null && node.getConfig().containsKey("storageRequirement")) {
            Object storage = node.getConfig().get("storageRequirement");
            if (storage instanceof Number) {
                return ((Number) storage).longValue();
            }
        }
        return 100 * 1024 * 1024L; // 100MB default
    }

    private ResourceEstimate.ConfidenceLevel calculateConfidenceLevel(ExecutionMetrics metrics) {
        if (metrics.getTotalExecutions() == 0) {
            return ResourceEstimate.ConfidenceLevel.LOW;
        } else if (metrics.getTotalExecutions() < 10) {
            return ResourceEstimate.ConfidenceLevel.MEDIUM;
        } else {
            return ResourceEstimate.ConfidenceLevel.HIGH;
        }
    }

    private ExecutionContext mergeContexts(ExecutionContext shared, ExecutionContext specific) {
        Map<String, Object> mergedVariables = new HashMap<>();
        if (shared.getVariables() != null) {
            mergedVariables.putAll(shared.getVariables());
        }
        if (specific.getVariables() != null) {
            mergedVariables.putAll(specific.getVariables());
        }

        Map<String, Object> mergedMetadata = new HashMap<>();
        if (shared.getMetadata() != null) {
            mergedMetadata.putAll(shared.getMetadata());
        }
        if (specific.getMetadata() != null) {
            mergedMetadata.putAll(specific.getMetadata());
        }

        return ExecutionContext.builder()
                .variables(mergedVariables)
                .metadata(mergedMetadata)
                .tenantId(specific.getTenantId() != null ? specific.getTenantId() : shared.getTenantId())
                .workflowRunId(
                        specific.getWorkflowRunId() != null ? specific.getWorkflowRunId() : shared.getWorkflowRunId())
                .nodeId(specific.getNodeId())
                .executionId(specific.getExecutionId() != null ? specific.getExecutionId() : shared.getExecutionId())
                .build();
    }

    @Override
    public ExecutionMetrics collectMetrics() {
        return new ExecutionMetrics(
                (int) metricsCollector.getTotalRequests(),
                (int) metricsCollector.getSuccessfulRequests(),
                (int) metricsCollector.getFailedRequests(),
                0, // pendingNodes
                0, // totalDurationMs
                (long) metricsCollector.getAverageLatencyMs());
    }

    @Override
    public Uni<NodeExecutionResult> replayNode(ExecutionContext historicalContext, NodeDescriptor node,
            ExecutionToken originalToken, ReplayOptions options) {

        return validateNotNull("historicalContext", historicalContext,
                validateNotNull("node", node,
                        validateNotNull("originalToken", originalToken, Uni.createFrom().deferred(() -> {

                            LOG.infof("Replaying node %s for run %s", node.getNodeId(),
                                    historicalContext.getWorkflowRunId());

                            // Check if token is still valid for replay (if configured)
                            // ReplayOptions doesn't have isValidateToken, using forceReplay as a proxy or
                            // just skipping
                            if (options != null && options.isForceReplay() && originalToken.isExpired()) {
                                return Uni.createFrom()
                                        .failure(new SecurityException("Original token expired, replay denied"));
                            }

                            // Re-execute the node with the historical context
                            return executeNode(historicalContext, node, originalToken)
                                    .onItem().transform(result -> {
                                        // Optional: Add replay metadata to the result
                                        return result;
                                    });
                        }))));
    }
}