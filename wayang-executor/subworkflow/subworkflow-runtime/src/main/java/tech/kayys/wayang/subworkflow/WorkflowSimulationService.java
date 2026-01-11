package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Simulation & What-If Analysis
 */
interface WorkflowSimulationService {

    /**
     * Simulate workflow execution without actually running it
     */
    Uni<SimulationResult> simulate(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> inputs,
        SimulationConfig config
    );

    /**
     * Estimate cost and duration
     */
    Uni<ExecutionEstimate> estimateExecution(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        Map<String, Object> inputs
    );

    /**
     * What-if analysis: "What if this node fails?"
     */
    Uni<WhatIfResult> whatIfAnalysis(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        List<WhatIfScenario> scenarios
    );
}