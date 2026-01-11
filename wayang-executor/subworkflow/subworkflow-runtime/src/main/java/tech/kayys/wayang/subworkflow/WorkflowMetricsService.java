package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Real-Time Workflow Metrics
 */
interface WorkflowMetricsService {

    /**
     * Record custom metric
     */
    void recordMetric(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        String metricName,
        double value,
        Map<String, String> tags
    );

    /**
     * Query metrics
     */
    Uni<List<MetricData>> queryMetrics(
        MetricQuery query
    );

    /**
     * Create dashboard
     */
    Uni<Dashboard> createDashboard(
        String name,
        List<Widget> widgets
    );
}