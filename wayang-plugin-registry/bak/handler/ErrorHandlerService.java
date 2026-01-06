
package tech.kayys.wayang.plugin.runtime.handler;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;


/**
 * Error Handler Service - Implements Error-as-Input Pattern
 * 
 * From Blueprint:
 * - Processes ErrorPayload from any node
 * - Makes routing decisions: retry, fallback, escalate, abort
 * - Uses CEL rules for policy-based decisions
 * - Integrates with HITL (Human-in-the-loop) for critical errors
 * - Supports self-healing attempts
 * - Maintains circuit breaker state
 * 
 * Decision Flow:
 * 1. Evaluate error type and retryability
 * 2. Check retry count against policy
 * 3. Apply CEL rules for custom logic
 * 4. Route to: retry, auto-fix, human-review, or abort
 */

@ApplicationScoped
public class ErrorHandlerService {

    private static final Logger LOG = Logger.getLogger(ErrorHandlerService.class);

    @Inject
    ErrorPolicyEngine policyEngine;

    @Inject
    RetryManager retryManager;

    @Inject
    HITLService hitlService;

    @Inject
    SelfHealingService selfHealingService;

    @Inject
    CircuitBreakerManager circuitBreakerManager;

    @Inject
    PluginAuditService auditService;

    /**
     * Handle error with policy-based routing
     */
    public Uni<ErrorHandlingDecision> handleError(
            ErrorPayload error, 
            NodeContext context) {
        
        LOG.infof("Handling error: type=%s, node=%s, attempt=%d/%d",
            error.getType(), error.getOriginNode(), 
            error.getAttempt(), error.getMaxAttempts());

        // Create error context for policy evaluation
        ErrorContext errorContext = ErrorContext.builder()
            .error(error)
            .nodeContext(context)
            .circuitBreakerState(
                circuitBreakerManager.getState(error.getOriginNode())
            )
            .build();

        // Evaluate error handling policies
        return policyEngine.evaluatePolicies(errorContext)
            .onItem().transformToUni(policyResult -> {
                
                ErrorHandlingDecision decision = determineAction(
                    error, 
                    policyResult, 
                    errorContext
                );

                // Log decision
                return auditService.logErrorHandling(error, decision)
                    .replaceWith(decision)
                    .onItem().transformToUni(d -> 
                        executeDecision(d, error, context)
                    );
            });
    }

    /**
     * Determine action based on error and policy
     */
    private ErrorHandlingDecision determineAction(
            ErrorPayload error,
            PolicyEvaluationResult policyResult,
            ErrorContext context) {
        
        // Check circuit breaker
        if (context.getCircuitBreakerState().isOpen()) {
            return ErrorHandlingDecision.builder()
                .action(ErrorAction.ABORT)
                .reason("Circuit breaker open for node: " + error.getOriginNode())
                .shouldRetry(false)
                .shouldEscalate(false)
                .build();
        }

        // Policy override
        if (policyResult.hasExplicitAction()) {
            return ErrorHandlingDecision.fromPolicy(policyResult);
        }

        // Check retry eligibility
        if (error.isRetryable() && 
            error.getAttempt() < error.getMaxAttempts()) {
            
            return ErrorHandlingDecision.builder()
                .action(ErrorAction.RETRY)
                .reason("Retryable error, attempt " + error.getAttempt())
                .shouldRetry(true)
                .delayMs(retryManager.calculateBackoff(error))
                .build();
        }

        // Check for auto-fix capability
        if (error.getType().equals("ValidationError") && 
            selfHealingService.canAutoFix(error)) {
            
            return ErrorHandlingDecision.builder()
                .action(ErrorAction.AUTO_FIX)
                .reason("Validation error - attempting auto-fix")
                .shouldRetry(false)
                .build();
        }

        // Escalate to human for critical errors
        if (shouldEscalateToHuman(error, policyResult)) {
            return ErrorHandlingDecision.builder()
                .action(ErrorAction.HUMAN_REVIEW)
                .reason("Critical error requiring human review")
                .shouldEscalate(true)
                .build();
        }

        // Default: abort
        return ErrorHandlingDecision.builder()
            .action(ErrorAction.ABORT)
            .reason("No recovery strategy available")
            .shouldRetry(false)
            .shouldEscalate(false)
            .build();
    }

    /**
     * Execute the error handling decision
     */
    private Uni<ErrorHandlingDecision> executeDecision(
            ErrorHandlingDecision decision,
            ErrorPayload error,
            NodeContext context) {
        
        return switch (decision.getAction()) {
            case RETRY -> {
                // Schedule retry with backoff
                yield retryManager.scheduleRetry(error, decision.getDelayMs())
                    .replaceWith(decision);
            }
            case AUTO_FIX -> {
                // Attempt automatic correction
                yield selfHealingService.attemptFix(error, context)
                    .onItem().transform(fixed -> {
                        if (fixed.isSuccess()) {
                            decision.setFixedInput(fixed.getResult());
                        } else {
                            // Auto-fix failed, escalate
                            decision.setAction(ErrorAction.HUMAN_REVIEW);
                            decision.setShouldEscalate(true);
                        }
                        return decision;
                    });
            }
            case HUMAN_REVIEW -> {
                // Create HITL task
                yield hitlService.createReviewTask(error, context)
                    .onItem().transform(taskId -> {
                        decision.setHitlTaskId(taskId);
                        return decision;
                    });
            }
            case FALLBACK -> {
                // Route to fallback node
                yield Uni.createFrom().item(decision);
            }
            case ABORT -> {
                // Record failure and abort
                circuitBreakerManager.recordFailure(error.getOriginNode());
                yield Uni.createFrom().item(decision);
            }
        };
    }

    /**
     * Determine if error should escalate to human
     */
    private boolean shouldEscalateToHuman(
            ErrorPayload error, 
            PolicyEvaluationResult policyResult) {
        
        // Security errors always escalate
        if (error.getType().equals("SecurityError")) {
            return true;
        }

        // Check policy threshold
        if (policyResult.getHumanReviewThreshold() != null) {
            return isAboveThreshold(
                error, 
                policyResult.getHumanReviewThreshold()
            );
        }

        // Multiple retry failures
        if (error.getAttempt() >= 3) {
            return true;
        }

        return false;
    }

    private boolean isAboveThreshold(ErrorPayload error, String threshold) {
        // Map error severity to threshold
        return switch (threshold) {
            case "CRITICAL" -> error.getType().equals("SecurityError");
            case "ERROR" -> true;
            default -> false;
        };
    }
}