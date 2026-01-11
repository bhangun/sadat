package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Intelligent Error Recovery
 */
interface IntelligentErrorRecovery {

    /**
     * Suggest error recovery actions
     */
    Uni<List<RecoveryAction>> suggestRecovery(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        tech.kayys.silat.core.domain.ErrorInfo error
    );

    /**
     * Auto-recover from common errors
     */
    Uni<Void> autoRecover(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        RecoveryStrategy strategy
    );

    /**
     * Learn from error patterns
     */
    Uni<Void> learnFromErrors(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        java.time.Duration learningPeriod
    );
}