package tech.kayys.wayang.standalone.orchestrator;

import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.core.execution.ExecutionResult;
import tech.kayys.wayang.core.execution.Status;
import tech.kayys.wayang.core.workflow.Edge;
import tech.kayys.wayang.core.workflow.NodeInstance;
import tech.kayys.wayang.core.workflow.WorkflowDefinition;
import tech.kayys.wayang.standalone.core.RuntimeContext;
import tech.kayys.wayang.standalone.executor.LocalNodeExecutor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Embedded orchestrator for standalone runtime.
 * Executes workflow DAG with minimal overhead.
 */
@Slf4j
public class EmbeddedOrchestrator {
    
    private final RuntimeContext context;
    private final LocalNodeExecutor executor;
    
    public EmbeddedOrchestrator(RuntimeContext context) {
        this.context = context;
        this.executor = new LocalNodeExecutor(context);
    }
    
    public ExecutionResult execute(WorkflowDefinition workflow) {
        log.info("Starting workflow execution");
        
        try {
            // Build DAG
            SimpleDAGExecutor dagExecutor = new SimpleDAGExecutor(
                workflow,
                executor,
                context
            );
            
            // Execute
            return dagExecutor.execute();
            
        } catch (Exception e) {
            log.error("Workflow execution failed", e);
            return ExecutionResult.builder()
                .status(Status.FAILED)
                .error(e.getMessage())
                .build();
        }
    }
}
