package tech.kayys.wayang.standalone.core;

import lombok.extern.slf4j.Slf4j;
import tech.kayys.wayang.core.execution.ExecutionResult;
import tech.kayys.wayang.core.workflow.WorkflowDefinition;
import tech.kayys.wayang.standalone.orchestrator.EmbeddedOrchestrator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for standalone agent runtime.
 * This class bootstraps the minimal runtime environment.
 */
@Slf4j
public class StandaloneAgentRuntime {
    
    private final RuntimeConfig config;
    private final EmbeddedOrchestrator orchestrator;
    private final RuntimeContext context;
    
    public StandaloneAgentRuntime() {
        this(RuntimeConfig.loadDefault());
    }
    
    public StandaloneAgentRuntime(RuntimeConfig config) {
        this.config = config;
        this.context = new RuntimeContext(config);
        this.orchestrator = new EmbeddedOrchestrator(context);
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
    }
    
    public static void main(String[] args) {
        log.info("Starting Wayang Standalone Agent Runtime...");
        
        try {
            StandaloneAgentRuntime runtime = new StandaloneAgentRuntime();
            runtime.start();
        } catch (Exception e) {
            log.error("Failed to start runtime", e);
            System.exit(1);
        }
    }
    
    public void start() throws Exception {
        log.info("Initializing runtime context...");
        context.initialize();
        
        log.info("Loading workflow definition...");
        WorkflowDefinition workflow = loadWorkflow();
        
        log.info("Executing workflow...");
        ExecutionResult result = orchestrator.execute(workflow);
        
        log.info("Workflow execution completed with status: {}", result.getStatus());
        
        if (config.isWaitForCompletion()) {
            // Keep running for async operations
            waitForCompletion();
        }
    }
    
    public void shutdown() {
        log.info("Shutting down runtime...");
        context.close();
        log.info("Runtime shutdown complete");
    }
    
    private WorkflowDefinition loadWorkflow() throws IOException {
        String workflowPath = config.getWorkflowPath();
        Path path = Paths.get(workflowPath);
        
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Workflow file not found: " + workflowPath);
        }
        
        String json = Files.readString(path);
        return context.getJsonUtil().fromJson(json, WorkflowDefinition.class);
    }
    
    private void waitForCompletion() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Runtime interrupted", e);
        }
    }
}