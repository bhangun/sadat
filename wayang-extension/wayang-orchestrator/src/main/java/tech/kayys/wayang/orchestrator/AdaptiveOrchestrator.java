@ApplicationScoped
public class AdaptiveOrchestrator implements WorkflowOrchestrator {
    @Inject EventBus eventBus;
    @Inject ExecutionStore executionStore;
    @Inject RuntimeHub runtimeHub;
    @Inject Scheduler scheduler;
    @Inject Checkpointer checkpointer;
    @Inject PolicyEnforcer policyEnforcer;
    @Inject A2AAdapter a2aAdapter;
    @Inject Replanner replanner;
    
    @Override
    public CompletableFuture<ExecutionRun> execute(ExecutionPlan plan, ExecutionRequest request) {
        // Validate plan
        policyEnforcer.validatePlan(plan, request);
        
        // Create execution run
        ExecutionRun run = executionStore.createRun(plan, request);
        
        // Emit plan.started event
        eventBus.emit(new PlanStartedEvent(run.getRunId(), plan.getPlanId()));
        
        // Build DAG and start execution
        DAGExecutor dagExecutor = new DAGExecutor(
            plan, run, runtimeHub, scheduler, checkpointer, eventBus
        );
        
        return dagExecutor.execute()
            .whenComplete((result, error) -> {
                if (error != null) {
                    handleExecutionError(run, error);
                } else {
                    handleExecutionComplete(run, result);
                }
            });
    }
    
    private void handleExecutionError(ExecutionRun run, Throwable error) {
        run.setStatus(RunStatus.FAILED);
        run.setError(error.getMessage());
        executionStore.updateRun(run);
        eventBus.emit(new PlanFailedEvent(run.getRunId(), error));
    }
    
    private void handleExecutionComplete(ExecutionRun run, ExecutionResult result) {
        run.setStatus(RunStatus.COMPLETED);
        run.setEndTime(Instant.now());
        executionStore.updateRun(run);
        eventBus.emit(new PlanCompletedEvent(run.getRunId(), result));
    }
}