package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.ErrorHandlingConfig;
import tech.kayys.wayang.schema.ErrorPayload;
import tech.kayys.wayang.schema.ErrorPayload.ErrorType;
import tech.kayys.wayang.schema.ExecutionContext;
import tech.kayys.wayang.schema.NodeDefinition;
import tech.kayys.wayang.schema.RetryPolicy;

import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.*;

/**
 * ErrorOrchestrator - Central error routing and recovery coordinator.
 * 
 * Implements the Error-as-Input semantic from the blueprint:
 * - Routes errors to appropriate handlers (retry, self-heal, HITL, fallback)
 * - Evaluates error handling policies using CEL expressions
 * - Manages error escalation thresholds
 * - Coordinates with SelfHealingService for auto-repair
 * - Maintains error provenance and audit trail
 * 
 * Design Principles:
 * - Deterministic error routing based on rules
 * - No side effects, only decision making
 * - Extensible policy evaluation
 * - Complete audit trail for compliance
 */
@ApplicationScoped
public class ErrorOrchestrator {

    private static final Logger LOG = Logger.getLogger(ErrorOrchestrator.class);

    @Inject
    CELEvaluator celEvaluator;

    @Inject
    SelfHealingService selfHealingService;

    @Inject
    ProvenanceService provenanceService;

    @Inject
    ErrorMetricsCollector metricsCollector;

    /**
     * Handle error and determine recovery action.
     * 
     * @param error   The error payload from failed node
     * @param nodeDef Node definition containing error handling config
     * @param context Execution context for decision making
     * @return ErrorDecision with recommended action
     */
    public Uni<ErrorDecision> handleError(
            ErrorPayload error,
            NodeDefinition nodeDef,
            ExecutionContext context) {

        LOG.debugf("Handling error for node %s: type=%s, retryable=%s",
                error.getOriginNode(),
                error.getType(),
                error.getRetryable());

        return Uni.createFrom().deferred(() -> {

            // 1. Log error to provenance
            return provenanceService.logError(error, nodeDef, context)
                    .onItem().transformToUni(v -> {

                        // 2. Record metrics
                        metricsCollector.recordError(error);

                        // 3. Get error handling configuration
                        ErrorHandlingConfig config = nodeDef.getErrorHandling() != null
                                ? nodeDef.getErrorHandling()
                                : ErrorHandlingConfig.defaultConfig();

                        // 4. Evaluate error handling rules
                        return evaluateErrorRules(error, config, context)
                                .onItem().transformToUni(decision -> {

                                    // 5. Log decision to audit
                                    return provenanceService.logErrorDecision(
                                            error,
                                            decision,
                                            context).replaceWith(decision);
                                });
                    });
        });
    }

    /**
     * Evaluate error handling rules to determine action.
     * Rules are evaluated in order:
     * 1. Circuit breaker check
     * 2. Retry policy evaluation
     * 3. Auto-fix eligibility
     * 4. Human review threshold
     * 5. Fallback availability
     * 6. Abort as last resort
     */
    private Uni<ErrorDecision> evaluateErrorRules(
            ErrorPayload error,
            ErrorHandlingConfig config,
            ExecutionContext context) {

        return Uni.createFrom().item(() -> {

            // Check circuit breaker
            if (config.getCircuitBreaker() != null &&
                    config.getCircuitBreaker().isEnabled()) {

                CircuitBreakerState cbState = checkCircuitBreaker(
                        error.getOriginNode(),
                        config.getCircuitBreaker(),
                        context);

                if (cbState == CircuitBreakerState.OPEN) {
                    LOG.warnf("Circuit breaker OPEN for node %s, aborting",
                            error.getOriginNode());

                    return ErrorDecision.builder()
                            .action(ErrorAction.ABORT)
                            .reason("Circuit breaker is open")
                            .metadata(Map.of("circuit_breaker", "open"))
                            .build();
                }
            }

            // Evaluate custom CEL rules if present
            if (config.getCustomRules() != null && !config.getCustomRules().isEmpty()) {
                ErrorDecision customDecision = evaluateCustomRules(
                        error,
                        config.getCustomRules(),
                        context);

                if (customDecision != null) {
                    return customDecision;
                }
            }

            // Rule 1: Check retry eligibility
            if (shouldRetry(error, config)) {
                return ErrorDecision.builder()
                        .action(ErrorAction.RETRY)
                        .reason("Error is retryable and within retry limit")
                        .metadata(Map.of(
                                "attempt", error.getAttempt() + 1,
                                "max_attempts", error.getMaxAttempts()))
                        .build();
            }

            // Rule 2: Check auto-fix eligibility
            if (shouldAutoFix(error, config)) {
                return ErrorDecision.builder()
                        .action(ErrorAction.AUTO_FIX)
                        .reason("Error type eligible for automatic fixing")
                        .metadata(Map.of("error_type", error.getType()))
                        .build();
            }

            // Rule 3: Check human review threshold
            if (shouldEscalateToHuman(error, config, context)) {
                return ErrorDecision.builder()
                        .action(ErrorAction.HUMAN_REVIEW)
                        .reason("Error severity exceeds human review threshold")
                        .metadata(Map.of(
                                "threshold", config.getHumanReviewThreshold(),
                                "error_count", context.getErrorCount()))
                        .build();
            }

            // Rule 4: Check fallback availability
            if (config.getFallbackNodeId() != null && !config.getFallbackNodeId().isEmpty()) {
                return ErrorDecision.builder()
                        .action(ErrorAction.FALLBACK)
                        .reason("Fallback node configured")
                        .metadata(Map.of("fallback_node", config.getFallbackNodeId()))
                        .build();
            }

            // Rule 5: Abort as last resort
            return ErrorDecision.builder()
                    .action(ErrorAction.ABORT)
                    .reason("No recovery strategy available")
                    .metadata(Map.of("error_type", error.getType()))
                    .build();
        });
    }

    /**
     * Evaluate custom CEL rules for error handling.
     */
    private ErrorDecision evaluateCustomRules(
            ErrorPayload error,
            List<ErrorHandlingRule> rules,
            ExecutionContext context) {

        Map<String, Object> variables = Map.of(
                "error", error,
                "context", context,
                "error_count", context.getErrorCount(),
                "has_previous_errors", !context.getErrorHistory().isEmpty());

        for (ErrorHandlingRule rule : rules) {
            try {
                Boolean matches = celEvaluator.evaluate(
                        rule.getCondition(),
                        variables,
                        Boolean.class);

                if (Boolean.TRUE.equals(matches)) {
                    LOG.debugf("Custom rule matched: %s → %s",
                            rule.getName(),
                            rule.getAction());

                    return ErrorDecision.builder()
                            .action(rule.getAction())
                            .reason("Matched custom rule: " + rule.getName())
                            .metadata(Map.of("rule", rule.getName()))
                            .build();
                }
            } catch (Exception e) {
                LOG.errorf(e, "Failed to evaluate custom rule: %s", rule.getName());
            }
        }

        return null; // No custom rule matched
    }

    /**
     * Check if error should be retried.
     */
    private boolean shouldRetry(ErrorPayload error, ErrorHandlingConfig config) {
        if (!error.getRetryable()) {
            return false;
        }

        RetryPolicy retryPolicy = config.getRetryPolicy();
        if (retryPolicy == null) {
            return false;
        }

        int attempt = error.getAttempt() != null ? error.getAttempt() : 0;
        int maxAttempts = retryPolicy.getMaxAttempts();

        return attempt < maxAttempts;
    }

    /**
     * Check if error should trigger auto-fix.
     */
    private boolean shouldAutoFix(ErrorPayload error, ErrorHandlingConfig config) {
        if (!config.isAutoHealEnabled()) {
            return false;
        }

        // Only certain error types are eligible for auto-fix
        return error.getType() == ErrorType.VALIDATION_ERROR ||
                error.getType() == ErrorType.LLM_ERROR ||
                (error.getSuggestedAction() == ErrorAction.AUTO_FIX);
    }

    /**
     * Check if error should be escalated to human review.
     */
    private boolean shouldEscalateToHuman(
            ErrorPayload error,
            ErrorHandlingConfig config,
            ExecutionContext context) {

        HumanReviewThreshold threshold = config.getHumanReviewThreshold();

        // Always escalate critical errors
        if (threshold == HumanReviewThreshold.CRITICAL &&
                error.getType() == ErrorType.SECURITY_ERROR) {
            return true;
        }

        // Escalate after multiple retries
        int attempt = error.getAttempt() != null ? error.getAttempt() : 0;
        if (attempt >= 2) {
            return true;
        }

        // Escalate if error count is high
        if (context.getErrorCount() >= 5) {
            return true;
        }

        // Escalate based on suggested action
        return error.getSuggestedAction() == ErrorAction.HUMAN_REVIEW;
    }

    /**
     * Check circuit breaker state for node.
     */
    private CircuitBreakerState checkCircuitBreaker(
            String nodeId,
            ErrorHandlingConfig.CircuitBreakerConfig config,
            ExecutionContext context) {

        // Count recent failures for this node
        long recentFailures = context.getErrorHistory().stream()
                .filter(e -> e.getOriginNode().equals(nodeId))
                .filter(e -> e.getTimestamp().isAfter(
                        Instant.now().minusMillis(config.getTimeoutMs())))
                .count();

        if (recentFailures >= config.getFailureThreshold()) {
            return CircuitBreakerState.OPEN;
        }

        // Check if recovering (half-open state)
        long recentSuccesses = context.getNodeResults().values().stream()
                .filter(r -> r.getNodeId().equals(nodeId))
                .filter(r -> r.isSuccess())
                .count();

        if (recentSuccesses >= config.getSuccessThreshold()) {
            return CircuitBreakerState.CLOSED;
        }

        return CircuitBreakerState.HALF_OPEN;
    }

    /**
     * Attempt to self-heal the error using LLM or deterministic repair.
     * 
     * @param nodeDef Node definition
     * @param context Execution context
     * @param error   Error to fix
     * @return HealedContext with fixed input if successful
     */
    public Uni<HealedContext> selfHeal(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error) {

        LOG.infof("Attempting self-healing for node %s, error type: %s",
                nodeDef.getId(),
                error.getType());

        return selfHealingService.heal(nodeDef, context, error)
                .onItem().transformToUni(healedContext -> {

                    // Log healing attempt
                    return provenanceService.logSelfHealing(
                            error,
                            healedContext,
                            context).replaceWith(healedContext);
                })
                .onFailure().recoverWithItem(th -> {
                    LOG.errorf(th, "Self-healing failed for node %s", nodeDef.getId());
                    return HealedContext.failed("Self-healing service error: " + th.getMessage());
                });
    }

    /**
     * Get error statistics for a workflow run.
     */
    public ErrorStatistics getErrorStatistics(ExecutionContext context) {
        Map<ErrorType, Long> errorsByType = context.getErrorHistory().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ErrorPayload::getType,
                        java.util.stream.Collectors.counting()));

        Map<String, Long> errorsByNode = context.getErrorHistory().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ErrorPayload::getOriginNode,
                        java.util.stream.Collectors.counting()));

        long retryableErrors = context.getErrorHistory().stream()
                .filter(ErrorPayload::getRetryable)
                .count();

        return ErrorStatistics.builder()
                .totalErrors(context.getErrorCount())
                .errorsByType(errorsByType)
                .errorsByNode(errorsByNode)
                .retryableErrors(retryableErrors)
                .build();
    }
}
