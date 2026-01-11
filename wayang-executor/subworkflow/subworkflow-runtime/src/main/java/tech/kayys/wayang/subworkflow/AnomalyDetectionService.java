package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Anomaly Detection
 */
interface AnomalyDetectionService {

    /**
     * Detect anomalies in workflow execution
     */
    Uni<List<Anomaly>> detectAnomalies(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        java.time.Duration timeWindow
    );

    /**
     * Create anomaly alert
     */
    Uni<Void> createAlert(
        AnomalyType type,
        AlertConfig config
    );

    /**
     * Train anomaly detection model
     */
    Uni<Void> trainModel(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        java.time.Duration trainingPeriod
    );
}