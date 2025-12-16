package tech.kayys.wayang.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.repository.WorkflowRepository;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.domain.Workflow.WorkflowStatus;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Autosave Manager
@ApplicationScoped
public class WorkflowAutosaveManager {
    private static final Logger logger = Logger.getLogger(WorkflowAutosaveManager.class);

    @Inject
    WorkflowRepository workflowRepository;

    private final Map<UUID, ScheduledFuture<?>> autosaveTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public void enableAutosave(UUID workflowId) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                () -> performAutosave(workflowId),
                30,
                30,
                TimeUnit.SECONDS);

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
            workflowRepository.findById(workflowId)
                    .subscribe().with(workflow -> {
                        if (workflow != null && workflow.status == WorkflowStatus.DRAFT) {
                            try {
                                // Create autosave snapshot
                                createAutosaveSnapshot(workflow);
                            } catch (Exception e) {
                                logger.error("Autosave failed in subscriber for workflow: " + workflowId, e);
                            }
                        }
                    }, failure -> {
                        logger.error("Failed to fetch workflow for autosave: " + workflowId, failure);
                    });
        } catch (Exception e) {
            // Log but don't throw
            logger.error("Autosave failed for workflow: " + workflowId, e);
        }
    }

    private void createAutosaveSnapshot(Workflow workflow) {
        // Implementation TODO: Save snapshot to DB
        logger.info("Autosaving workflow snapshot for: " + workflow.id);
    }
}