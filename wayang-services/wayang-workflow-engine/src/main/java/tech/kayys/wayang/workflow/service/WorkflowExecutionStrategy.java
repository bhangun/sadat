package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import java.util.Map;

/**
 * WorkflowExecutionStrategy - Interface for different workflow execution approaches
 * 
 * This interface allows for different execution strategies (DAG, state machines, event-driven, etc.)
 * making the workflow engine more use case agnostic.
 */
public interface WorkflowExecutionStrategy {
    
    /**
     * Execute a workflow with the given inputs
     * @param workflow The workflow definition to execute
     * @param inputs The input data for the workflow
     * @param context The execution context
     * @return A Uni containing the execution result
     */
    Uni<WorkflowRun> execute(WorkflowDefinition workflow, Map<String, Object> inputs, ExecutionContext context);
    
    /**
     * Get the strategy type identifier
     * @return The strategy type
     */
    String getStrategyType();
    
    /**
     * Check if this strategy can handle a specific workflow
     * @param workflow The workflow to check
     * @return true if this strategy can handle the workflow
     */
    boolean canHandle(WorkflowDefinition workflow);
}