package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow CLI
 */
interface WorkflowCLI {

    /**
     * Initialize new workflow project
     */
    void init(String projectName, String template);

    /**
     * Deploy workflow
     */
    void deploy(String workflowFile, String environment);

    /**
     * Run workflow locally
     */
    void run(String workflowId, String inputFile);

    /**
     * Generate client SDK
     */
    void generateSDK(String workflowId, String language);

    /**
     * Workflow linting
     */
    void lint(String workflowFile);
}