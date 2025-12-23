package tech.kayys.wayang.workflow.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.runtime.StartupEvent;
import org.jboss.logging.Logger;

/**
 * WorkflowEngineInitializer - Initializes the workflow engine with default strategies and configurations
 * 
 * This class ensures that all necessary components are properly initialized
 * when the workflow engine starts up.
 */
@ApplicationScoped
public class WorkflowEngineInitializer {

    private static final Logger LOG = Logger.getLogger(WorkflowEngineInitializer.class);

    @Inject
    NodeExecutorRegistry nodeExecutorRegistry;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Initializing Workflow Engine with use case agnostic capabilities");
        
        // Register built-in node executors if not already registered
        initializeNodeExecutors();
        
        LOG.info("Workflow Engine initialized successfully");
    }

    private void initializeNodeExecutors() {
        // The registry has PostConstruct method that registers built-ins
        // We just ensure they're properly configured here
        LOG.infof("NodeExecutorRegistry initialized with %d built-in executors", 
                 nodeExecutorRegistry.listBuiltIn().size());
    }
}