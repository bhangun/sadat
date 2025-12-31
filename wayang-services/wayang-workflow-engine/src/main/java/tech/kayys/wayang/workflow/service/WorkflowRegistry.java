package tech.kayys.wayang.workflow.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

@ApplicationScoped
public class WorkflowRegistry {

    // In-memory storage for workflow definitions
    private final Map<String, WorkflowDefinition> workflows = new ConcurrentHashMap<>();
    private final Map<String, WorkflowDefinition> workflowVersions = new ConcurrentHashMap<>();

    /**
     * Register a new workflow definition
     */
    public Uni<WorkflowDefinition> register(WorkflowDefinition workflow) {
        workflows.put(workflow.getId().getValue(), workflow);

        // Also store with version for version-specific retrieval
        String versionedId = workflow.getId().getValue() + ":" + workflow.getVersion();
        workflowVersions.put(versionedId, workflow);

        return Uni.createFrom().item(workflow);
    }

    /**
     * Get workflow by ID
     */
    public Uni<WorkflowDefinition> getWorkflow(String workflowId) {
        if (workflowId == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Workflow ID cannot be null"));
        }

        WorkflowDefinition workflow = workflows.get(workflowId);
        if (workflow == null) {
            return Uni.createFrom().nullItem(); // Will be handled by composer with proper error
        }

        return Uni.createFrom().item(workflow);
    }

    /**
     * Get workflow by ID and version
     */
    public Uni<WorkflowDefinition> getWorkflowByVersion(String workflowId, String version) {
        String versionedId = workflowId + ":" + version;
        WorkflowDefinition workflow = workflowVersions.get(versionedId);

        if (workflow == null) {
            return Uni.createFrom().nullItem();
        }

        return Uni.createFrom().item(workflow);
    }

    /**
     * Get latest version of a workflow
     */
    public Uni<WorkflowDefinition> getLatestVersion(String workflowId) {
        // For simplicity, return the version in the main map (most recently registered)
        return getWorkflow(workflowId);
    }

    /**
     * List all workflows for a tenant
     */
    public Uni<List<WorkflowDefinition>> getAllWorkflows() {
        return Uni.createFrom().item(List.copyOf(workflows.values()));
    }

    /**
     * Activate a workflow version (make it available for execution)
     */
    public Uni<Void> activate(String workflowId, String version) {
        String versionedId = workflowId + ":" + version;
        WorkflowDefinition workflow = workflowVersions.get(versionedId);

        if (workflow == null) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "Workflow not found: " + workflowId + " version " + version));
        }

        // In a real implementation, there would be an 'active' flag
        // For this implementation, we just ensure it exists
        return Uni.createFrom().voidItem();
    }

    /**
     * Deactivate a workflow version (prevent further executions)
     */
    public Uni<Void> deactivate(String workflowId, String version) {
        String versionedId = workflowId + ":" + version;
        WorkflowDefinition workflow = workflowVersions.get(versionedId);

        if (workflow == null) {
            return Uni.createFrom().failure(new IllegalArgumentException(
                    "Workflow not found: " + workflowId + " version " + version));
        }

        // In a real implementation, there would be an 'active' flag
        return Uni.createFrom().voidItem();
    }

    /**
     * Delete a workflow definition
     */
    public Uni<Void> delete(String workflowId, String version) {
        String versionedId = workflowId + ":" + version;

        workflows.remove(workflowId);
        workflowVersions.remove(versionedId);

        return Uni.createFrom().voidItem();
    }

    /**
     * Import workflow from file/content
     */
    public Uni<WorkflowDefinition> importWorkflow(String workflowId, String content) {
        // In a real implementation, this would parse content and create a workflow
        // For now, return a basic workflow definition
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id(workflowId)
                .name("Imported Workflow: " + workflowId)
                .version("1.0")
                .description("Workflow imported from content")
                .build();

        return register(workflow);
    }

    /**
     * Export workflow as serializable format
     */
    public Uni<String> exportWorkflow(String workflowId) {
        return getWorkflow(workflowId).map(workflow -> {
            if (workflow == null) {
                return null;
            }
            // In a real implementation, this would serialize the workflow
            // For now, just return a simple representation
            return workflow.toString();
        });
    }

    /**
     * Search workflows by criteria
     */
    public Uni<List<WorkflowDefinition>> search(String tenantId, String namePattern) {
        List<WorkflowDefinition> result = workflows.values().stream()
                .filter(wf -> namePattern == null || wf.getName().toLowerCase().contains(namePattern.toLowerCase()))
                .toList();

        return Uni.createFrom().item(result);
    }

    /**
     * Validate workflow structure
     */
    public Uni<Boolean> validate(String workflowId) {
        return getWorkflow(workflowId).map(workflow -> {
            if (workflow == null) {
                return false;
            }

            // Basic validation checks
            if (workflow.getId() == null || workflow.getId().getValue() == null
                    || workflow.getId().getValue().trim().isEmpty()) {
                return false;
            }

            // More complex validations would go here (acyclic, connected nodes, etc.)
            return true;
        });
    }
}