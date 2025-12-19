package tech.kayys.wayang.workflow.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

/**
 * Dynamic Workflow Generation (AI-powered)
 */
@ApplicationScoped
public class DynamicWorkflowGenerator {

    @Inject
    @RestClient
    LLMClient llmClient;

    @Inject
    WorkflowParser workflowParser;

    @Inject
    WorkflowOptimizer workflowOptimizer;

    /**
     * Generate workflow from natural language
     */
    public Uni<WorkflowDefinition> generateFromPrompt(String prompt) {
        return llmClient.complete(
                buildGenerationPrompt(prompt)).map(response -> {
                    // Parse LLM response into workflow definition
                    return workflowParser.parse(response);
                });
    }

    /**
     * Optimize existing workflow
     */
    public Uni<WorkflowDefinition> optimizeWorkflow(
            WorkflowDefinition workflow) {
        return llmClient.complete(
                buildOptimizationPrompt(workflow)).map(response -> {
                    // Apply suggested optimizations
                    return workflowOptimizer.apply(workflow, response);
                });
    }
}
