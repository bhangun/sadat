package tech.kayys.silat.executor.subworkflow;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.silat.core.domain.TenantId;
import tech.kayys.silat.core.domain.WorkflowRun;
import tech.kayys.silat.core.domain.WorkflowRunId;
import tech.kayys.silat.core.domain.WorkflowRunSnapshot;
import tech.kayys.silat.core.engine.WorkflowRunManager;
import tech.kayys.silat.core.domain.ErrorInfo;
import tech.kayys.silat.core.domain.NodeExecution;
import tech.kayys.silat.core.domain.RunStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Sub-workflow monitor
 */
@ApplicationScoped
class SubWorkflowMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(SubWorkflowMonitor.class);

    @Inject
    WorkflowRunManager runManager;

    /**
     * Wait for sub-workflow completion with timeout
     */
    public Uni<SubWorkflowResult> waitForCompletion(
            WorkflowRunId childRunId,
            TenantId tenantId,
            Duration timeout) {

        LOG.debug("Monitoring sub-workflow: childRunId={}, timeout={}s",
            childRunId.value(), timeout.getSeconds());

        Instant deadline = Instant.now().plus(timeout);

        return pollForCompletion(childRunId, tenantId, deadline);
    }

    /**
     * Poll for completion
     */
    private Uni<SubWorkflowResult> pollForCompletion(
            WorkflowRunId childRunId,
            TenantId tenantId,
            Instant deadline) {

        return runManager.getRun(childRunId, tenantId)
            .flatMap(run -> {
                if (isTerminal(run.getStatus())) {
                    // Completed
                    return Uni.createFrom().item(createResult(run));
                }

                // Check timeout
                if (Instant.now().isAfter(deadline)) {
                    return Uni.createFrom().failure(
                        new java.util.concurrent.TimeoutException("Sub-workflow execution timed out"));
                }

                // Continue polling
                return Uni.createFrom().item(0)
                    .onItem().delayIt().by(Duration.ofMillis(500))
                    .flatMap(v -> pollForCompletion(childRunId, tenantId, deadline));
            });
    }

    private boolean isTerminal(RunStatus status) {
        return status == RunStatus.COMPLETED ||
               status == RunStatus.FAILED ||
               status == RunStatus.CANCELLED;
    }

    private SubWorkflowResult createResult(WorkflowRun run) {
        WorkflowRunSnapshot snapshot = run.createSnapshot();

        Duration duration = snapshot.startedAt() != null && snapshot.completedAt() != null ?
            Duration.between(snapshot.startedAt(), snapshot.completedAt()) :
            Duration.ZERO;

        ErrorInfo error = null;
        if (snapshot.status() == RunStatus.FAILED) {
            // Extract error from failed nodes
            error = snapshot.nodeExecutions().values().stream()
                .filter(exec -> exec.getLastError() != null)
                .map(NodeExecution::getLastError)
                .findFirst()
                .orElse(new ErrorInfo("UNKNOWN_ERROR", "Sub-workflow failed", "", Map.of()));
        }

        return new SubWorkflowResult(
            snapshot.status(),
            snapshot.variables(),
            error,
            duration
        );
    }
}