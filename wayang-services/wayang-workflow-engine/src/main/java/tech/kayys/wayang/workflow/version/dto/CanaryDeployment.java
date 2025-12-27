package tech.kayys.wayang.workflow.version.dto;

import java.time.Instant;

/**
 * CanaryDeployment: Tracks canary deployment state.
 */
public class CanaryDeployment {
    private final String deploymentId;
    private final String versionId;
    private final int percentage;
    private final Instant startedAt;
    private final CanaryStatus status;

    public CanaryDeployment(
            String deploymentId,
            String versionId,
            int percentage) {
        this.deploymentId = deploymentId;
        this.versionId = versionId;
        this.percentage = percentage;
        this.startedAt = Instant.now();
        this.status = CanaryStatus.ACTIVE;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getVersionId() {
        return versionId;
    }

    public int getPercentage() {
        return percentage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public CanaryStatus getStatus() {
        return status;
    }
}
