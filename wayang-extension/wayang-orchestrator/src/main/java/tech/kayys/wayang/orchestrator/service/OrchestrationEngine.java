package tech.kayys.wayang.orchestrator.engine;

import tech.kayys.wayang.common.domain.*;
import tech.kayys.wayang.common.event.*;
import io.smallrye.mutiny.Uni;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class OrchestrationEngine {
    
    private static final Logger LOG = Logger.getLogger(OrchestrationEngine.class);
    
    @Inject
    DAGWalker dagWalker;
    
    @Inject
    StateManager stateManager;
    
    @Inject
    ErrorHandler errorHandler;
    
    @Inject
    EventEmitter eventEmitter;
    
    /**
     * Execute workflow plan
     */
    public Uni<ExecutionRun> execute(ExecutionPlan plan, ExecutionContext context) {
        LOG.infof("Starting execution for plan: %s", plan.planId());
        
        return stateManager.createRun(plan, context)
            .flatMap(run -> {
                // Emit plan started event
                eventEmitter.emit(new PlanStartedEvent(
                    UUID.randomUUID().toString(),
                    run.runId(),
                    Instant.now(),
                    context.traceId()
                ));
                
                // Walk DAG and execute nodes
                return dagWalker.walk(plan, run)
                    .onFailure().recoverWithItem(failure -> {
                        LOG.error("Execution failed", failure);
                        return run.withStatus(RunStatus.FAILED);
                    });
            });
    }
    
    /**
     * Handle node completion
     */
    @ConsumeEvent("node.completed")
    public Uni<Void> onNodeCompleted(NodeCompletedEvent event) {
        LOG.debugf("Node completed: %s", event.nodeId());
        
        return stateManager.getNodeState(event.runId(), event.nodeId())
            .flatMap(state -> {
                if (state.result().status() == ExecutionResult.Status.ERROR) {
                    return errorHandler.handleError(event.runId(), state);
                }
                
                // Continue to next nodes
                return dagWalker.continueExecution(event.runId());
            });
    }
    
    /**
     * Handle execution errors
     */
    @ConsumeEvent("node.error")
    public Uni<Void> onNodeError(NodeErrorEvent event) {
        LOG.warnf("Node error: %s - %s", event.nodeId(), event.error().message());
        
        return errorHandler.handleError(event.runId(), event.nodeId(), event.error())
            .replaceWithVoid();
    }
}