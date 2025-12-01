
// Autosave Manager
@ApplicationScoped
public class WorkflowAutosaveManager {
    @Inject WorkflowRepository workflowRepository;
    
    private final Map<UUID, ScheduledFuture<?>> autosaveTasks = 
        new ConcurrentHashMap<>();
    
    private final ScheduledExecutorService scheduler = 
        Executors.newScheduledThreadPool(10);
    
    public void enableAutosave(UUID workflowId) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
            () -> performAutosave(workflowId),
            30,
            30,
            TimeUnit.SECONDS
        );
        
        autosaveTasks.put(workflowId, task);
    }
    
    public void disableAutosave(UUID workflowId) {
        ScheduledFuture<?> task = autosaveTasks.remove(workflowId);
        if (task != null) {
            task.cancel(false);
        }
    }
    
    private void performAutosave(UUID workflowId) {
        try {
            Workflow workflow = workflowRepository.findById(workflowId)
                .orElse(null);
            
            if (workflow != null && workflow.getStatus() == WorkflowStatus.DRAFT) {
                // Create autosave snapshot
                createAutosaveSnapshot(workflow);
            }
        } catch (Exception e) {
            // Log but don't throw
            logger.error("Autosave failed for workflow: " + workflowId, e);
        }
    }
}