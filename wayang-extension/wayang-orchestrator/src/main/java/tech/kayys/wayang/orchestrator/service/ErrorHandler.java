package tech.kayys.wayang.orchestrator.engine;

import tech.kayys.wayang.common.domain.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ErrorHandler {
    
    private static final Logger LOG = Logger.getLogger(ErrorHandler.class);
    
    @Inject
    StateManager stateManager;
    
    @Inject
    EventEmitter eventEmitter;
    
    public Uni<ErrorHandlingDecision> handleError(
        String runId, 
        String nodeId, 
        ErrorPayload error
    ) {
        LOG.warnf("Handling error for node %s: %s", nodeId, error.message());
        
        return stateManager.getNodeState(runId, nodeId)
            .flatMap(state -> evaluateErrorPolicy(state, error))
            .flatMap(decision -> applyDecision(runId, nodeId, decision));
    }
    
    private Uni<ErrorHandlingDecision> evaluateErrorPolicy(
        NodeState state, 
        ErrorPayload error
    ) {
        // Check retry policy
        if (shouldRetry(error, state)) {
            return Uni.createFrom().item(
                ErrorHandlingDecision.retry(error.maxAttempts())
            );
        }
        
        // Check if auto-fix applicable
        if (error.type() == ErrorPayload.ErrorType.VALIDATION_ERROR) {
            return Uni.createFrom().item(ErrorHandlingDecision.autoFix());
        }
        
        // Escalate to human if critical
        if (error.suggestedAction() == ErrorPayload.SuggestedAction.HUMAN_REVIEW) {
            return Uni.createFrom().item(ErrorHandlingDecision.humanReview());
        }
        
        // Default: abort
        return Uni.createFrom().item(ErrorHandlingDecision.abort());
    }
    
    private boolean shouldRetry(ErrorPayload error, NodeState state) {
        return error.retryable() && 
               state.attempt() < error.maxAttempts();
    }
    
    private Uni<ErrorHandlingDecision> applyDecision(
        String runId, 
        String nodeId, 
        ErrorHandlingDecision decision
    ) {
        return switch (decision.action()) {
            case RETRY -> 
                stateManager.scheduleRetry(runId, nodeId, decision.delayMs())
                    .replaceWith(decision);
            
            case AUTO_FIX -> 
                invokeSelfHealing(runId, nodeId)
                    .replaceWith(decision);
            
            case HUMAN_REVIEW -> 
                createHumanTask(runId, nodeId)
                    .replaceWith(decision);
            
            case ABORT -> 
                stateManager.failNode(runId, nodeId)
                    .replaceWith(decision);
            
            default -> Uni.createFrom().item(decision);
        };
    }
    
    private Uni<Void> invokeSelfHealing(String runId, String nodeId) {
        // Delegate to self-healing service
        return Uni.createFrom().voidItem();
    }
    
    private Uni<String> createHumanTask(String runId, String nodeId) {
        // Create HITL task
        return Uni.createFrom().item("task-" + UUID.randomUUID());
    }
}