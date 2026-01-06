package tech.kayys.wayang.workflow.kernel;

import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.sdk.dto.ExecutionMetrics;

/**
 * WorkflowEngine with instrumentation and monitoring capabilities
 */
public interface InstrumentedWorkflowEngine extends WorkflowEngine, ManagedWorkflowComponent {

        /**
         * Execute node with detailed telemetry
         */
        Uni<InstrumentedNodeExecutionResult> executeNodeWithTelemetry(
                        ExecutionContext context,
                        NodeDescriptor node,
                        ExecutionToken token,
                        TelemetryConfig telemetryConfig);

        /**
         * Get execution statistics for a node type
         */
        Uni<NodeExecutionStats> getNodeExecutionStats(String nodeType);

        /**
         * Record a custom metric
         */
        void recordMetric(String metricName, double value, Map<String, String> tags);

        public record InstrumentedNodeExecutionResult(
                        NodeExecutionResult result,
                        ExecutionMetrics metrics,
                        List<PerformanceMarker> markers) {
        }

        public record NodeExecutionStats(
                        String nodeType,
                        long totalExecutions,
                        long successfulExecutions,
                        double averageDurationMs,
                        Map<String, Long> errorCounts) {
        }

        public record TelemetryConfig(
                        boolean enableTracing,
                        boolean enableMetrics,
                        boolean enableLogging,
                        Map<String, String> additionalTags) {
        }
}
