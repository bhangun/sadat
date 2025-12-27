package tech.kayys.wayang.workflow.version.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.version.dto.CanaryDeployment;
import tech.kayys.wayang.workflow.version.dto.CanaryMetrics;

import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CanaryDeploymentManager {

    private static final Logger LOG = Logger.getLogger(CanaryDeploymentManager.class);

    private final Map<String, CanaryDeployment> activeCanaries = new ConcurrentHashMap<>();

    private final Map<String, CanaryMetrics> canaryMetrics = new ConcurrentHashMap<>();

    /**
     * Deploy a canary version with specified traffic percentage.
     */
    public Uni<CanaryDeployment> deploy(String versionId, int percentage) {
        if (percentage < 1 || percentage > 100) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException(
                            "Canary percentage must be between 1 and 100"));
        }

        String deploymentId = UUID.randomUUID().toString();
        CanaryDeployment deployment = new CanaryDeployment(
                deploymentId, versionId, percentage);

        activeCanaries.put(versionId, deployment);

        LOG.infof("Started canary deployment %s for version %s at %d%%",
                deploymentId, versionId, percentage);

        return Uni.createFrom().item(deployment);
    }

    /**
     * Get metrics for a canary deployment.
     */
    public Uni<CanaryMetrics> getMetrics(String versionId) {
        CanaryMetrics metrics = canaryMetrics.get(versionId);

        if (metrics == null) {
            // Return empty metrics if none recorded yet
            metrics = new CanaryMetrics(0, 0, 0, 0.0, 0.0);
        }

        return Uni.createFrom().item(metrics);
    }

    /**
     * Record execution result for canary.
     */
    public void recordExecution(
            String versionId,
            boolean success,
            long latency) {

        canaryMetrics.compute(versionId, (key, existing) -> {
            if (existing == null) {
                return new CanaryMetrics(
                        1,
                        success ? 1 : 0,
                        success ? 0 : 1,
                        latency,
                        success ? 0.0 : 1.0);
            }

            int total = existing.totalExecutions() + 1;
            int successful = existing.successfulExecutions() + (success ? 1 : 0);
            int failed = existing.failedExecutions() + (success ? 0 : 1);
            double avgLatency = (existing.averageLatency() * existing.totalExecutions() + latency) / total;
            double errorRate = (double) failed / total;

            return new CanaryMetrics(total, successful, failed, avgLatency, errorRate);
        });
    }
}