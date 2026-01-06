package tech.kayys.wayang.workflow.kernel;

/**
 * Response for a workflow trigger request.
 */
public record TriggerWorkflowResponse(String runId, String status) {
}
