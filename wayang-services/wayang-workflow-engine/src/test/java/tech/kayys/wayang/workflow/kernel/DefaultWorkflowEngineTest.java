package tech.kayys.wayang.workflow.kernel;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.kayys.wayang.sdk.dto.ExecutionMetrics;
import tech.kayys.wayang.workflow.executor.NodeExecutor;
import tech.kayys.wayang.workflow.executor.NodeExecutorRegistry;
import tech.kayys.wayang.workflow.executor.NodeExecutionResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class DefaultWorkflowEngineTest {

    @Inject
    DefaultWorkflowEngine workflowEngine;

    @InjectMock
    NodeExecutorRegistry executorRegistry;

    @InjectMock
    NodeExecutor nodeExecutor;

    @InjectMock
    ExecutionMetricsCollector metricsCollector;

    @InjectMock
    RetryPolicyManager retryPolicyManager;

    private ExecutionContext context;
    private NodeDescriptor node;
    private ExecutionToken token;

    @BeforeEach
    void setup() {
        context = ExecutionContext.builder()
                .workflowRunId("run-123")
                .tenantId("tenant-1")
                .executionId("exec-123")
                .build();

        node = Mockito.mock(NodeDescriptor.class);
        when(node.getNodeId()).thenReturn("node-1");
        when(node.getType()).thenReturn("test-type");

        token = Mockito.mock(ExecutionToken.class);
        when(token.isExpired()).thenReturn(false);

        when(executorRegistry.getExecutor(anyString())).thenReturn(nodeExecutor);
    }

    @Test
    @DisplayName("Should execute a single node")
    void testExecuteNode() {
        NodeExecutionResult successResult = NodeExecutionResult.builder()
                .nodeId("node-1")
                .status(NodeExecutionResult.Status.SUCCESS)
                .output(Map.of("out", "val"))
                .build();
        when(nodeExecutor.execute(any(), any())).thenReturn(Uni.createFrom().item(successResult));

        workflowEngine.executeNode(context, node, token)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();
    }

    @Test
    @DisplayName("Should execute batch nodes")
    void testExecuteBatchNodes() {
        NodeExecutionRequest request = NodeExecutionRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .node(node)
                .context(context)
                .token(token)
                .build();

        NodeExecutionResult nodeResult = NodeExecutionResult.builder()
                .nodeId("node-1")
                .status(NodeExecutionResult.Status.SUCCESS)
                .output(Map.of("out", "val"))
                .build();
        when(nodeExecutor.execute(any(), any())).thenReturn(Uni.createFrom().item(nodeResult));

        workflowEngine.executeNodes(List.of(request), context)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();
    }

    @Test
    @DisplayName("Should collect metrics")
    void testCollectMetrics() {
        when(metricsCollector.getTotalRequests()).thenReturn(10L);
        when(metricsCollector.getSuccessfulRequests()).thenReturn(8L);
        when(metricsCollector.getFailedRequests()).thenReturn(2L);
        when(metricsCollector.getAverageLatencyMs()).thenReturn(100.0);
        when(metricsCollector.getErrorDistribution()).thenReturn(Map.of());

        ExecutionMetrics metrics = workflowEngine.collectMetrics();

        assertNotNull(metrics);
        assertEquals(10, metrics.totalNodes());
        assertEquals(8, metrics.completedNodes());
        assertEquals(2, metrics.failedNodes());
        assertEquals(100, (int) metrics.avgNodeDurationMs());
    }

    @Test
    @DisplayName("Should estimate resources")
    void testEstimateResources() {
        when(node.getType()).thenReturn("compute-intensive");
        when(metricsCollector.collectForNode(anyString()))
                .thenReturn(new tech.kayys.wayang.sdk.dto.ExecutionMetrics(0, 0, 0, 0, 0, 0L));

        ResourceEstimate estimate = workflowEngine.estimateResources(context, node);

        assertNotNull(estimate);
        assertEquals(ResourceEstimate.ConfidenceLevel.LOW, estimate.getConfidence());
    }
}
