package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Optimization & Recommendations
 */
interface WorkflowOptimizationService {

    /**
     * Analyze workflow for performance bottlenecks
     */
    Uni<OptimizationReport> analyzeWorkflow(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId
    );

    /**
     * Suggest optimizations
     */
    Uni<List<Optimization>> suggestOptimizations(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId
    );

    /**
     * Auto-optimize workflow
     */
    Uni<tech.kayys.silat.core.domain.WorkflowDefinition> autoOptimize(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        OptimizationGoals goals
    );
}