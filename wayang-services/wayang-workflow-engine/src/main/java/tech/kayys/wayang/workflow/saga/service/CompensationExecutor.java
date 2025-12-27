package tech.kayys.wayang.workflow.saga.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.saga.model.CompensationAction;

/**
 * Compensation Executor
 */
@ApplicationScoped
public class CompensationExecutor {

    private static final Logger log = LoggerFactory.getLogger(CompensationExecutor.class);

    public Uni<Void> execute(
            WorkflowRun run,
            String nodeId,
            CompensationAction action) {
        log.info("Executing compensation for node {} in run {}", nodeId, run.getRunId());

        // Execute compensation action based on type
        return switch (action.actionType()) {
            case "rollback" -> executeRollback(run, nodeId, action.parameters());
            case "notify" -> executeNotification(run, nodeId, action.parameters());
            case "cleanup" -> executeCleanup(run, nodeId, action.parameters());
            default -> {
                log.warn("Unknown compensation action: {}", action.actionType());
                yield Uni.createFrom().voidItem();
            }
        };
    }

    private Uni<Void> executeRollback(
            WorkflowRun run,
            String nodeId,
            Map<String, Object> params) {
        // Implement rollback logic
        log.info("Rolling back node {} in run {}", nodeId, run.getRunId());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> executeNotification(
            WorkflowRun run,
            String nodeId,
            Map<String, Object> params) {
        // Send notification about compensation
        log.info("Sending compensation notification for node {} in run {}",
                nodeId, run.getRunId());
        return Uni.createFrom().voidItem();
    }

    private Uni<Void> executeCleanup(
            WorkflowRun run,
            String nodeId,
            Map<String, Object> params) {
        // Cleanup resources
        log.info("Cleaning up resources for node {} in run {}", nodeId, run.getRunId());
        return Uni.createFrom().voidItem();
    }
}