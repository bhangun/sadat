package tech.kayys.wayang.workflow.kernel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.executor.NodeExecutorRegistry;
import tech.kayys.wayang.workflow.repository.WorkflowRepository;
import tech.kayys.wayang.workflow.repository.WorkflowRunRepository;

/**
 * Default implementation of the stateless workflow engine.
 */
@ApplicationScoped
public class DefaultWorkflowEngine implements WorkflowEngine {

        private static final Logger LOG = Logger.getLogger(DefaultWorkflowEngine.class);

        @Inject
        NodeExecutorRegistry executorRegistry;

        @Inject
        ExecutionTokenValidator tokenValidator;

        @Override
        public Uni<NodeExecutionResult> executeNode(
                        ExecutionContext context,
                        NodeDescriptor node,
                        ExecutionToken token) {

                LOG.infof("Executing node: %s (implementation: %s)",
                                node.getNodeId(), node.getImplementation());

                // 1. Validate token
                return tokenValidator.validate(token)
                                .flatMap(isValid -> {
                                        if (!isValid) {
                                                return Uni.createFrom().item(createInvalidTokenResult(node, context));
                                        }

                                        // 2. Find executor
                                        return executorRegistry.getExecutor(node.getImplementation())
                                                        .onItem().ifNull()
                                                        .failWith(
                                                                        () -> new IllegalArgumentException(
                                                                                        "No executor for: " + node
                                                                                                        .getImplementation()))
                                                        .flatMap(executor -> {
                                                                // 3. Execute
                                                                Instant start = Instant.now();
                                                                return executor.execute(context, node, token)
                                                                                .onItem()
                                                                                .transform(result -> enrichResult(
                                                                                                result, start))
                                                                                .onFailure().recoverWithItem(
                                                                                                failure -> handleExecutionFailure(
                                                                                                                failure,
                                                                                                                node,
                                                                                                                context,
                                                                                                                start));
                                                        });
                                });
        }

        @Override
        public Uni<NodeExecutionResult> dryRunNode(
                        ExecutionContext context,
                        NodeDescriptor node) {

                LOG.infof("Dry-running node: %s", node.getNodeId());

                // Create a mock token for dry-run
                ExecutionToken dryRunToken = new SimpleExecutionToken(
                                context.getRunId(),
                                "dry-run-" + UUID.randomUUID(),
                                0,
                                Instant.now(),
                                Instant.now().plus(Duration.ofMinutes(5)),
                                "dry-run-signature");

                return executeNode(context, node, dryRunToken);
        }

        @Override
        public Uni<NodeExecutionResult> replayNode(
                        ExecutionContext historicalContext,
                        NodeDescriptor node,
                        ExecutionToken originalToken,
                        ReplayOptions options) {

                LOG.infof("Replaying node: %s (original attempt: %d)",
                                node.getNodeId(), originalToken.getAttempt());

                // Ensure idempotency
                ExecutionToken replayToken = originalToken.forRetry(
                                originalToken.getAttempt() + 1000 // Offset to avoid collision
                );

                // Mark context as replay
                ExecutionContext markedContext = historicalContext
                                .withMetadata("replay", true)
                                .withMetadata("originalToken", originalToken.getNodeExecutionId())
                                .withMetadata("replayOptions", options.toMap());

                return executeNode(markedContext, node, replayToken);
        }

        @Override
        public Uni<ValidationResult> validateExecution(
                        ExecutionContext context,
                        NodeDescriptor node) {

                return executorRegistry.getExecutor(node.getImplementation())
                                .onItem().ifNull()
                                .failWith(() -> new IllegalArgumentException(
                                                "No executor for: " + node.getImplementation()))
                                .flatMap(executor -> executor.validate(context, node))
                                .onFailure()
                                .recoverWithItem(failure -> ValidationResult
                                                .failure("Validation failed: " + failure.getMessage()));
        }

        private NodeExecutionResult createInvalidTokenResult(
                        NodeDescriptor node,
                        ExecutionContext context) {

                return NodeExecutionResult.failure(
                                node.getNodeId(),
                                ExecutionError.system(
                                                "INVALID_TOKEN",
                                                "Execution token is invalid or expired",
                                                false,
                                                null),
                                context,
                                Duration.ZERO);
        }

        private NodeExecutionResult handleExecutionFailure(
                        Throwable failure,
                        NodeDescriptor node,
                        ExecutionContext context,
                        Instant startTime) {

                LOG.errorf(failure, "Node execution failed: %s", node.getNodeId());
                Duration duration = Duration.between(startTime, Instant.now());

                ExecutionError error;
                if (failure instanceof IllegalArgumentException) {
                        error = ExecutionError.business(
                                        "INVALID_CONFIG",
                                        failure.getMessage(),
                                        false,
                                        null);
                } else {
                        error = ExecutionError.system(
                                        "EXECUTION_FAILED",
                                        "Internal execution error: " + failure.getMessage(),
                                        true, // Assume system errors are retriable
                                        null);
                }

                return NodeExecutionResult.failure(
                                node.getNodeId(),
                                error,
                                context,
                                duration);
        }

        private NodeExecutionResult enrichResult(
                        NodeExecutionResult result,
                        Instant startTime) {

                Duration duration = Duration.between(startTime, Instant.now());
                Map<String, Object> metadata = new HashMap<>(result.getMetadata());
                metadata.put("durationMs", duration.toMillis());
                metadata.put("enrichedAt", Instant.now().toString());

                return new NodeExecutionResult(result.getNodeId(), result.getState(), result.getOutput(),
                                result.getMetadata());
        }
}
